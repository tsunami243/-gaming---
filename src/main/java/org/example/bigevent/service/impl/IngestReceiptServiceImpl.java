package org.example.bigevent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bigevent.mapper.IngestContentMapper;
import org.example.bigevent.mapper.IngestReceiptMapper;
import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.ReadingMode;
import org.example.bigevent.pojo.WorkflowStatus;
import org.example.bigevent.pojo.dto.IngestAcceptance;
import org.example.bigevent.pojo.dto.IngestEvent;
import org.example.bigevent.pojo.dto.IngestReceipt;
import org.example.bigevent.service.IngestReceiptService;
import org.example.bigevent.utils.JsonTags;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HexFormat;

/**
 * 采集事件入库实现：以「回执幂等 + 文章落库」的单个事务承载外部采集数据。
 *
 * <p>幂等由 {@code ingest_receipt.event_id} 唯一键保证：同一 eventId 重复投递时，
 * 只要载荷指纹一致就直接返回既有回执（{@code created=false}），指纹不一致则返回 409 冲突。</p>
 *
 * <p>文章去重另有一条链路：按 {@code canonical_url_hash}（原文 URL 的 SHA-256）查重，
 * 命中则复用既有文章，避免同一篇公告被不同来源或不同批次重复入库。</p>
 */
@Service
public class IngestReceiptServiceImpl implements IngestReceiptService {

    private final IngestReceiptMapper receiptMapper;
    private final IngestContentMapper contentMapper;

    /**
     * 构造采集入库服务。
     *
     * @param receiptMapper 采集回执读写
     * @param contentMapper 文章/正文/关联的写入与种子查询
     */
    public IngestReceiptServiceImpl(IngestReceiptMapper receiptMapper, IngestContentMapper contentMapper) {
        this.receiptMapper = receiptMapper;
        this.contentMapper = contentMapper;
    }

    /**
     * 计算事件的规范 JSON 的 SHA-256，作为载荷指纹。
     *
     * @param event 采集事件
     * @return 十六进制小写指纹
     * @throws IllegalStateException 事件无法序列化或摘要算法不可用时抛出
     */
    @Override
    public String payloadHash(IngestEvent event) {
        try {
            byte[] json = new ObjectMapper().writeValueAsBytes(event);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (Exception exception) {
            throw new IllegalStateException("cannot hash ingest event", exception);
        }
    }

    /**
     * 幂等地接收一条采集事件，并在同一事务内落库为草稿文章。
     *
     * @param event       采集事件
     * @param fingerprint 载荷指纹，用于识别「同 eventId 但内容不同」的冲突
     * @return 接收结果，含回执与是否首次创建
     * @throws ResponseStatusException eventId 已存在但指纹不一致时返回 409
     */
    @Override
    @Transactional
    public IngestAcceptance accept(IngestEvent event, String fingerprint) {
        IngestReceipt existing = receiptMapper.find(event.eventId());
        if (existing != null) {
            if (!existing.payloadHash().equals(fingerprint)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "eventId payload conflict");
            }
            return new IngestAcceptance(existing, false);
        }

        receiptMapper.insert(event.eventId(), fingerprint, null, event.decision(), event.ruleVersion());
        // REJECT 只留回执作为审计凭证，不产生文章
        if (!"REJECT".equals(event.decision())) {
            receiptMapper.attachArticle(event.eventId(), resolveOrCreateArticle(event));
        }
        return new IngestAcceptance(receiptMapper.find(event.eventId()), true);
    }

    /**
     * 解析或新建文章：先按规范化 URL 查重，未命中才创建。
     *
     * <p>创建时会补齐来源（content_source）、分类（按首个 gameCode 映射）、
     * 游戏关联（article_game）与正文副本（article_content）。</p>
     *
     * @param event 采集事件
     * @return 文章 ID（既有或新建）
     * @throws IllegalStateException 来源/分类/游戏种子数据缺失时抛出
     */
    private Integer resolveOrCreateArticle(IngestEvent event) {
        String canonicalUrlHash = sha256(event.originalUrl().trim());
        Integer existingArticleId = contentMapper.findArticleIdByCanonicalUrlHash(canonicalUrlHash);
        if (existingArticleId != null) {
            return existingArticleId;
        }

        Long sourceId = contentMapper.findSourceId(event.sourceCode());
        if (sourceId == null) {
            contentMapper.ensureSource(event.sourceCode(), sourceName(event), baseUrl(event.originalUrl()), sourceLanguage(event));
            sourceId = contentMapper.findSourceId(event.sourceCode());
        }
        if (sourceId == null) {
            throw new IllegalStateException("unable to create or locate content source");
        }

        Integer categoryId = contentMapper.findCategoryId(event.gameCodes().get(0));
        Integer systemUserId = contentMapper.findIngestSystemUserId();
        if (categoryId == null || systemUserId == null) {
            throw new IllegalStateException("first-release seed data is missing");
        }

        Article article = new Article();
        article.setTitle(event.title());
        article.setSummary(event.summary());
        // The crawled body is stored in the legacy content column so the public v1
        // detail can serve it directly; article_content keeps the hashed copy.
        article.setContent(storedBody(event));
        article.setState("草稿");
        article.setCategoryId(categoryId);
        article.setSourceId(sourceId);
        article.setOriginalUrl(event.originalUrl());
        article.setCanonicalUrlHash(canonicalUrlHash);
        article.setOriginalTitle(event.title());
        article.setSourceLanguage(sourceLanguage(event));
        article.setIngestedAt(OffsetDateTime.parse(event.fetchedAt()).toLocalDateTime());
        if (event.originalPublishedAt() != null && !event.originalPublishedAt().isBlank()) {
            article.setOriginalPublishedAt(OffsetDateTime.parse(event.originalPublishedAt()).toLocalDateTime());
        }
        if (event.originalPublishedDate() != null) {
            article.setOriginalPublishedDate(LocalDate.parse(event.originalPublishedDate()));
        }
        article.setContentType(event.contentType());
        article.setReadingMode(ReadingMode.FULL_TEXT.name());
        // 采集入库一律落在待审核区，只有 ACCEPT 才不带复核原因码
        article.setWorkflowStatus(WorkflowStatus.PENDING_REVIEW.name());
        article.setWorkflowReasonCode("ACCEPT".equals(event.decision()) ? null : "REQUIRES_REVIEW");
        article.setCreateUser(systemUserId);
        article.setTags(JsonTags.encode(event.tags()));
        contentMapper.insertArticle(article);

        if (event.content() != null && !event.content().isBlank()) {
            contentMapper.insertArticleContent(article.getId(), event.content(), sha256(event.content()));
        }
        for (int i = 0; i < event.gameCodes().size(); i++) {
            Integer gameId = contentMapper.findEnabledGameId(event.gameCodes().get(i));
            if (gameId == null) {
                throw new IllegalStateException("enabled game seed is missing");
            }
            // 首个 gameCode 标记为主游戏
            contentMapper.insertArticleGame(article.getId(), gameId, i == 0);
        }
        return article.getId();
    }

    /**
     * 取入库正文：{@code article.content} 为 NOT NULL，正文缺失或空白时回退为摘要。
     *
     * @param event 采集事件
     * @return 待写入 content 列的正文
     */
    private static String storedBody(IngestEvent event) {
        return event.content() == null || event.content().isBlank() ? event.summary() : event.content();
    }

    /**
     * 取来源展示名，缺失时回退为来源编码。
     *
     * @param event 采集事件
     * @return 非空来源名
     */
    private static String sourceName(IngestEvent event) {
        return event.sourceName() == null || event.sourceName().isBlank() ? event.sourceCode() : event.sourceName();
    }

    /**
     * 取内容语言，缺失时默认 {@code zh-CN}。
     *
     * @param event 采集事件
     * @return 非空语言标签
     */
    private static String sourceLanguage(IngestEvent event) {
        return event.sourceLanguage() == null || event.sourceLanguage().isBlank() ? "zh-CN" : event.sourceLanguage();
    }

    /**
     * 从原文 URL 截取站点根（scheme + host），作为来源 base_url。
     *
     * @param originalUrl 原文 URL
     * @return 形如 {@code https://df.qq.com} 的站点根
     */
    private static String baseUrl(String originalUrl) {
        URI uri = URI.create(originalUrl);
        return uri.getScheme() + "://" + uri.getAuthority();
    }

    /**
     * 计算字符串的 SHA-256（十六进制小写）。
     *
     * @param value 待摘要文本
     * @return 十六进制指纹
     * @throws IllegalStateException 摘要算法不可用时抛出
     */
    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
