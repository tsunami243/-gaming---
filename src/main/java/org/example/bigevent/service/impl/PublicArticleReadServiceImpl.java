package org.example.bigevent.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.example.bigevent.mapper.ArticleMapper;
import org.example.bigevent.mapper.ArticleSourceMapper;
import org.example.bigevent.mapper.GameMapper;
import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.ReadingMode;
import org.example.bigevent.pojo.dto.ArticleDetailResponse;
import org.example.bigevent.pojo.dto.ArticleGameRow;
import org.example.bigevent.pojo.dto.ArticleSourceRow;
import org.example.bigevent.pojo.dto.ArticleSummaryResponse;
import org.example.bigevent.pojo.dto.GameResponse;
import org.example.bigevent.pojo.dto.PageResponse;
import org.example.bigevent.service.PublicArticleReadService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 公开读取的业务实现。
 *
 * <p>所有公开读取规则都在这一层：参数校验、白名单、DTO 组装、批量关联查询、
 * 以及把入库正文转换成可安全渲染的纯文本。数据库访问一律通过 mapper 完成。</p>
 *
 * <p>安全边界：只有 {@code FULL_TEXT} 读法模式才返回正文，且正文会被剥成纯文本
 * （前端用插值渲染，杜绝 {@code v-html} 带来的 XSS 面）。</p>
 */
@Service
public class PublicArticleReadServiceImpl implements PublicArticleReadService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> ALLOWED_GAMES = Set.of("delta-force", "valorant", "cs2", "apex-legends");
    private static final Set<String> CONTENT_TYPES = Set.of("announcement", "patch_notes", "event", "new_content", "guide", "esports");
    private static final int SUMMARY_FALLBACK_LENGTH = 120;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern BLOCK_CLOSE = Pattern.compile("(?i)</(?:p|div|h[1-6]|li|section|article|tr|blockquote)>");
    private static final Pattern LINE_BREAK = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern ANY_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern TRAILING_SPACES = Pattern.compile("[ \\t]+\\n");
    private static final Pattern EXTRA_BLANK_LINES = Pattern.compile("\\n{3,}");
    private static final Pattern ENTITY = Pattern.compile("&(lt|gt|quot|#39|apos|nbsp|amp);");

    private final GameMapper gameMapper;
    private final ArticleMapper articleMapper;
    private final ArticleSourceMapper sourceMapper;

    /**
     * 构造公开读取服务。
     *
     * @param gameMapper   游戏与文章-游戏关联查询
     * @param articleMapper 文章主表查询
     * @param sourceMapper 来源名查询
     */
    public PublicArticleReadServiceImpl(GameMapper gameMapper, ArticleMapper articleMapper, ArticleSourceMapper sourceMapper) {
        this.gameMapper = gameMapper;
        this.articleMapper = articleMapper;
        this.sourceMapper = sourceMapper;
    }

    /**
     * 查询已启用的游戏（即前端的「分类」列表）。
     *
     * @return 游戏列表，别名会被解析成标签数组
     */
    @Override
    public List<GameResponse> listGames() {
        return gameMapper.findEnabled().stream()
                .map(game -> new GameResponse(game.code(), game.nameZh(), game.nameOriginal(),
                        parseTags(game.aliases()), Boolean.TRUE.equals(game.enabled())))
                .toList();
    }

    /**
     * 分页查询已发布文章的摘要列表，可按游戏与内容类型筛选。
     *
     * <p>先校验分页范围与筛选值白名单（不合法抛 422），再用 PageHelper 分页，
     * 最后批量补全「文章-游戏」关联与来源名，避免 N+1 查询。</p>
     *
     * @param pageNum     页码，需 ≥ 1
     * @param pageSize    每页条数，需在 1~50 之间
     * @param gameCode    游戏编码，需在白名单内；空表示不筛选
     * @param contentType 内容类型，需在枚举内；空表示不筛选
     * @return 分页结果（含总数）
     * @throws ResponseStatusException 分页或筛选参数非法时返回 422
     */
    @Override
    public PageResponse<ArticleSummaryResponse> listArticles(int pageNum, int pageSize, String gameCode, String contentType) {
        validatePage(pageNum, pageSize);
        validateGameCode(gameCode);
        validateContentType(contentType);

        PageHelper.startPage(pageNum, pageSize);
        List<Article> articles;
        try {
            articles = articleMapper.listPublishedByFilters(blankToNull(gameCode), blankToNull(contentType));
        } finally {
            // 无论查询成败都要清掉线程绑定的分页参数，避免污染后续查询
            PageHelper.clearPage();
        }

        long total = articles instanceof Page<?> page ? page.getTotal() : articles.size();
        List<Integer> ids = articles.stream().map(Article::getId).toList();
        Map<Integer, List<String>> articleGames = gamesFor(ids);
        Map<Integer, String> sources = sourcesFor(ids);
        List<ArticleSummaryResponse> items = articles.stream()
                .map(article -> summary(article, articleGames.getOrDefault(article.getId(), List.of()), sources.get(article.getId())))
                .toList();
        return new PageResponse<>(items, total, pageNum, pageSize);
    }

    /**
     * 查询已发布文章的详情；正文仅在 {@code FULL_TEXT} 读法模式下返回。
     *
     * @param id 文章 ID
     * @return 详情 DTO，正文为已剥标签的纯文本（无正文时为 {@code null}）
     * @throws ResponseStatusException ID 非法或文章不可见时返回 404
     */
    @Override
    public ArticleDetailResponse articleDetail(int id) {
        if (id < 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        }
        Article article = articleMapper.selectPublishedContentById(id);
        if (article == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        }
        ArticleSummaryResponse summary = summary(article,
                gamesFor(List.of(id)).getOrDefault(id, List.of()), sourcesFor(List.of(id)).get(id));
        // Only the full-text reading mode exposes the stored body; other modes stay summary-only.
        String content = ReadingMode.FULL_TEXT.name().equals(article.getReadingMode())
                ? toPlainText(article.getContent()) : null;
        return new ArticleDetailResponse(summary.id(), summary.title(), summary.summary(), summary.gameCodes(),
                summary.contentType(), summary.readingMode(), summary.workflowStatus(), summary.tags(),
                summary.sourceName(), summary.sourceLanguage(), summary.originalPublishedAt(),
                summary.originalPublishedDate(), summary.ingestedAt(), summary.publishedAt(),
                summary.coverImg(), article.getOriginalUrl(), content);
    }

    /** 元数据与摘要组装；列表与详情共用，避免两处口径不一致。 */
    private ArticleSummaryResponse summary(Article article, List<String> gameCodes, String sourceName) {
        String summary = article.getSummary();
        if (summary == null || summary.isBlank()) {
            summary = stripHtml(article.getContent());
            if (summary.length() > SUMMARY_FALLBACK_LENGTH) summary = summary.substring(0, SUMMARY_FALLBACK_LENGTH);
        }
        return new ArticleSummaryResponse(article.getId(), article.getTitle(), summary, gameCodes,
                article.getContentType(), article.getReadingMode(), article.getWorkflowStatus(), parseTags(article.getTags()),
                sourceName, article.getSourceLanguage(), article.getOriginalPublishedAt(),
                article.getOriginalPublishedDate(), article.getIngestedAt(), article.getPublishedAt(), article.getCoverImg());
    }

    /**
     * 入库正文是带块级标签的清洗 HTML；公开端只渲染纯文本，因此在这里做标签剥离。
     *
     * <p><b>顺序很关键：先解码实体，再剥标签。</b>若反序，正文里的
     * {@code &lt;img src=x onerror=...&gt;} 会先原样保留、解码后变成真正的标签语法并返回给
     * 客户端——消费方一旦改用 {@code v-html}/富文本渲染即构成存储型 XSS。先解码再剥，
     * 可保证出口不含任何 {@code <...>} 结构。</p>
     *
     * <p>保证的边界：{@link #ANY_TAG} 需要闭合的 {@code >}，因此像 {@code "a < b"} 这种
     * 孤立尖括号会原样留下。HTML5 解析器对未闭合标签按文本处理，风险很低，
     * 但消费方若要拿到"绝不含 {@code <}"的严格保证，需在此再做一次实体化处理。</p>
     *
     * @param storedBody 入库正文（可能为 null 或空白）
     * @return 规整后的纯文本；无有效内容时返回 {@code null}
     */
    private static String toPlainText(String storedBody) {
        if (storedBody == null || storedBody.isBlank()) return null;
        String text = BLOCK_CLOSE.matcher(storedBody).replaceAll("\n\n");
        text = LINE_BREAK.matcher(text).replaceAll("\n");
        text = decodeEntities(text);
        text = ANY_TAG.matcher(text).replaceAll("");
        text = TRAILING_SPACES.matcher(text).replaceAll("\n");
        text = EXTRA_BLANK_LINES.matcher(text).replaceAll("\n\n");
        return text.trim().isEmpty() ? null : text.trim();
    }

    /**
     * 单次解码 HTML 实体：{@code &amp;} 放在最后处理，避免 {@code &amp;lt;} 被二次解码成 {@code <}。
     *
     * @param value 含实体的文本
     * @return 解码后的文本
     */
    private static String decodeEntities(String value) {
        Matcher matcher = ENTITY.matcher(value);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String replacement = switch (matcher.group(1)) {
                case "lt" -> "<";
                case "gt" -> ">";
                case "quot" -> "\"";
                case "#39", "apos" -> "'";
                case "nbsp" -> " ";
                default -> "&";
            };
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * 校验游戏编码是否在白名单内（空值放行，表示不筛选）。
     *
     * @param gameCode 待校验游戏编码
     * @throws ResponseStatusException 不在白名单内时返回 422
     */
    private void validateGameCode(String gameCode) {
        if (gameCode != null && !gameCode.isBlank() && !ALLOWED_GAMES.contains(gameCode)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "unsupported gameCode");
        }
    }

    /**
     * 校验内容类型是否在允许枚举内（空值放行，表示不筛选）。
     *
     * @param contentType 待校验内容类型
     * @throws ResponseStatusException 不在枚举内时返回 422
     */
    private void validateContentType(String contentType) {
        if (contentType != null && !contentType.isBlank() && !CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "unsupported contentType");
        }
    }

    /**
     * 校验分页参数范围。
     *
     * @param pageNum  页码，需 ≥ 1
     * @param pageSize 每页条数，需在 1~{@link #MAX_PAGE_SIZE} 之间
     * @throws ResponseStatusException 超出范围时返回 422
     */
    private void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "pageNum must be >= 1 and pageSize must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    /**
     * 批量查询文章的来源展示名。
     *
     * @param ids 文章 ID 列表
     * @return 文章 ID → 来源名；空入参返回空 Map
     */
    private Map<Integer, String> sourcesFor(List<Integer> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<Integer, String> names = new HashMap<>();
        for (ArticleSourceRow source : sourceMapper.findNames(ids)) names.put(source.articleId(), source.sourceName());
        return names;
    }

    /**
     * 批量查询文章关联的游戏编码。
     *
     * @param articleIds 文章 ID 列表
     * @return 文章 ID → 游戏编码列表；空入参返回空 Map
     */
    private Map<Integer, List<String>> gamesFor(List<Integer> articleIds) {
        if (articleIds.isEmpty()) return Map.of();
        Map<Integer, List<String>> games = new HashMap<>();
        for (ArticleGameRow row : gameMapper.findArticleGames(articleIds)) {
            games.computeIfAbsent(row.articleId(), key -> new ArrayList<>()).add(row.code());
        }
        return games;
    }

    /**
     * 把数据库中的 JSON 标签串解析为字符串列表。
     *
     * @param rawTags JSON 数组文本
     * @return 标签列表；空值或解析失败时返回空列表（不抛异常，保证读取接口高可用）
     */
    private List<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) return List.of();
        try {
            return OBJECT_MAPPER.readValue(rawTags, new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    /**
     * 粗暴剥标签（摘要兜底用）；与 {@link #toPlainText(String)} 的区别是它不做块级换行还原。
     *
     * @param html 原始 HTML
     * @return 去标签后的文本，null 输入返回空串
     */
    private static String stripHtml(String html) {
        return html == null ? "" : ANY_TAG.matcher(html).replaceAll("").replaceAll("&[^;]+;", " ").trim();
    }

    /**
     * 空白串归一为 {@code null}，便于 Mapper 的 {@code <if>} 判空。
     *
     * @param value 原始值
     * @return 去空白后为空的返回 {@code null}，否则返回原值
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
