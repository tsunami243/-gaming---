package org.example.bigevent.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.example.bigevent.mapper.ArticleLikeMapper;
import org.example.bigevent.mapper.ArticleMapper;
import org.example.bigevent.mapper.CommentMapper;
import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.PageBean;
import org.example.bigevent.service.ArticleFavoriteService;
import org.example.bigevent.service.ArticleService;
import org.example.bigevent.service.HeatService;
import org.example.bigevent.service.ViewHistoryService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章业务实现（legacy 模型）：承载文章增删改查、轮播、热度榜与互动统计。
 *
 * <p>权限模型：操作者身份来自 {@link ThreadLocalUtil} 中的 JWT 声明。
 * 具备 {@code ROLE_CONTENT_ADMIN} / {@code ROLE_SUPER_ADMIN} 者可跨用户操作，
 * 普通用户只能改自己的文章。</p>
 *
 * <p>注意：公开读取的新链路（{@code PublicArticleReadService}）不经过本类；
 * 本类主要服务于 legacy 控制器与热度/统计相关能力。</p>
 */
@Service
public class ArticleServicelmpl implements ArticleService {

    private static final String PUBLISHED_STATE = "已发布";

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ViewHistoryService viewHistoryService;

    @Autowired
    private ArticleFavoriteService articleFavoriteService;

    @Autowired
    private HeatService heatService;

    /**
     * 新增文章（作者取当前登录用户）。
     *
     * @param article 文章实体
     */
    @Override
    public void addArticle(Article article) {
        Map<String, Object> map = ThreadLocalUtil.get();
        addArticle(article, (Integer) map.get("userId"));
    }

    /**
     * 新增文章并指定作者：补齐时间戳，封面缺失时兜底为空串。
     *
     * @param article    文章实体
     * @param createUser 作者用户 ID
     */
    @Override
    public void addArticle(Article article, Integer createUser) {
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        article.setCreateUser(createUser);
        if (article.getCoverImg() == null) {
            article.setCoverImg("");
        }
        articleMapper.add(article);
    }

    /**
     * 分页查询文章列表。公开列表不按用户过滤，只按分类与「已发布」状态筛选。
     *
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @param categoryId 分类 ID，为 null 时不筛选
     * @param state      该参数被有意忽略——列表强制只看已发布文章
     * @return 分页结果
     */
    @Override
    public PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state) {
        PageBean<Article> pageBean = new PageBean<>();

        PageHelper.startPage(pageNum, pageSize);
        // 公开列表不按用户过滤，只按分类和状态筛选
        List<Article> list = articleMapper.list(categoryId, PUBLISHED_STATE, null);
        fillPage(pageBean, list);
        return pageBean;
    }

    /**
     * 按标签搜索已发布文章（分页）。
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param tag      标签关键字
     * @return 分页结果
     */
    @Override
    public PageBean<Article> search(Integer pageNum, Integer pageSize, String tag) {
        PageBean<Article> pageBean = new PageBean<>();

        PageHelper.startPage(pageNum, pageSize);
        List<Article> list = articleMapper.searchByTag(tag, PUBLISHED_STATE);
        fillPage(pageBean, list);
        return pageBean;
    }

    /**
     * 更新文章：校验存在性与归属，管理员走 updateByAdmin、普通作者走 update。
     *
     * @param article 待更新文章（需携带 id）
     * @throws RuntimeException          文章不存在时抛出
     * @throws ResponseStatusException   非管理员尝试改他人文章时返回 403
     */
    @Override
    public void update(Article article) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        Article existing = articleMapper.detail(article.getId());
        if (existing == null) {
            throw new RuntimeException("文章不存在");
        }
        boolean contentAdmin = isContentAdmin(map);
        if (!contentAdmin && !userId.equals(existing.getCreateUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权修改他人文章");
        }
        article.setUpdateTime(LocalDateTime.now());
        article.setCreateUser(contentAdmin ? existing.getCreateUser() : userId);
        if (article.getCoverImg() == null) {
            article.setCoverImg("");
        }
        if (contentAdmin) {
            articleMapper.updateByAdmin(article);
        } else {
            articleMapper.update(article);
        }
    }

    /**
     * 查询文章详情（仅已发布可见）。
     *
     * @param id 文章 ID
     * @return 文章实体，未发布或不存在时返回 {@code null}
     */
    @Override
    public Article detail(Integer id) {
        return articleMapper.detailPublished(id);
    }

    /**
     * 断言文章已发布可见，否则抛 404。用于在读取互动数据前做存在性门禁。
     *
     * @param id 文章 ID
     * @throws ResponseStatusException 文章不可见时返回 404
     */
    @Override
    public void requirePublished(Integer id) {
        if (articleMapper.detailPublished(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        }
    }

    /**
     * 删除文章：管理员可删除任意文章，普通用户只能删除自己创建的。
     *
     * @param id 文章 ID
     * @throws ResponseStatusException 文章不存在返回 404、无权删除返回 403
     */
    @Override
    public void delete(Integer id) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        Article existing = articleMapper.detail(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        }
        if (isContentAdmin(map)) {
            articleMapper.deleteById(id);
            return;
        }
        if (!userId.equals(existing.getCreateUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权删除他人文章");
        }
        articleMapper.delete(id, userId);
    }

    /**
     * 查询轮播位文章。
     *
     * @param limit 条数上限
     * @return 轮播文章列表
     */
    @Override
    public List<Article> getCarouselList(Integer limit) {
        return articleMapper.selectCarouselList(limit);
    }

    /**
     * 热度排行榜 TopN：ZSet 优先，Redis 不可用或榜单为空时降级按 MySQL heat 排序。
     *
     * <p>返回顺序严格按热度榜的 ID 顺序重排，并回填实时分数。</p>
     *
     * @param limit 条数上限
     * @return 按热度降序的文章列表，无数据时为空列表
     */
    @Override
    public List<Article> getHotList(Integer limit) {
        List<Integer> ids = heatService.topN(limit);
        if (ids == null || ids.isEmpty()) {
            ids = articleMapper.selectHotIdsFromDb(limit);
        }
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<Article> articles = articleMapper.selectPublishedByIds(ids);
        Map<Integer, Article> byId = new HashMap<>();
        for (Article article : articles) {
            byId.put(article.getId(), article);
        }

        List<Article> ordered = new ArrayList<>();
        for (Integer id : ids) {
            Article article = byId.get(id);
            if (article == null) {
                continue;
            }
            Integer score = heatService.score(id);
            if (score != null) {
                article.setHeat(score);
            }
            ordered.add(article);
        }
        return ordered;
    }

    /**
     * 统计文章的浏览量、独立浏览量、收藏量、点赞量、评论量（取自独立统计表）。
     *
     * @param articleId 文章 ID
     * @return 统计 Map；文章不存在时返回 {@code null}
     */
    @Override
    public Map<String, Object> getArticleStats(Integer articleId) {
        Article article = articleMapper.findById(articleId);
        if (article == null) {
            return null;
        }
        Map<String, Object> stats = new HashMap<>();
        // 浏览相关来自独立浏览记录表
        stats.put("views", viewHistoryService.getViewCount(articleId));
        stats.put("uniqueViews", viewHistoryService.getUniqueViewCount(articleId));
        // 收藏来自独立收藏表
        stats.put("favorites", articleFavoriteService.getFavoriteCount(articleId));
        stats.put("likeCount", articleLikeMapper.countByArticleId(articleId));
        stats.put("commentCount", commentMapper.listByArticleId(articleId).size());
        return stats;
    }

    /**
     * 计算文章热度：ZSet 命中直接返回实时值，否则按各互动指标加权求和。
     *
     * <p>权重公式：浏览量×1 + 独立浏览×3 + 收藏×5 + 点赞×2 + 评论×3。</p>
     *
     * @param articleId 文章 ID
     * @return 热度分；文章不存在时返回 0
     */
    @Override
    public Integer calculateHeat(Integer articleId) {
        // ZSet 优先：命中直接返回（Redis 为实时权威）
        Integer heatScore = heatService.score(articleId);
        if (heatScore != null) {
            return heatScore;
        }
        Article article = articleMapper.findById(articleId);
        if (article == null) {
            return 0;
        }
        int views = viewHistoryService.getViewCount(articleId);
        int uniqueViews = viewHistoryService.getUniqueViewCount(articleId);
        int favorites = articleFavoriteService.getFavoriteCount(articleId);
        int likeCount = articleLikeMapper.countByArticleId(articleId);
        int commentCount = commentMapper.listByArticleId(articleId).size();
        // 热度计算公式：浏览量*1 + 独立浏览*3 + 收藏*5 + 点赞*2 + 评论*3
        return views * 1 + uniqueViews * 3 + favorites * 5 + likeCount * 2 + commentCount * 3;
    }

    /**
     * 把 MyBatis-PageHelper 的查询结果（或普通 List）装入分页对象。
     *
     * @param pageBean 目标分页对象
     * @param list     查询结果，可能是 {@link Page} 也可能是普通 List
     */
    @SuppressWarnings("unchecked")
    private static void fillPage(PageBean<Article> pageBean, List<Article> list) {
        if (list instanceof Page) {
            Page<Article> page = (Page<Article>) list;
            pageBean.setTotal(page.getTotal());
            pageBean.setItems(page.getResult());
        } else {
            pageBean.setTotal((long) list.size());
            pageBean.setItems(list);
        }
    }

    /**
     * 判断当前登录用户是否具备内容管理角色。
     *
     * @param claims JWT 声明，可为 null
     * @return 是内容管理员或超级管理员时返回 {@code true}
     */
    private static boolean isContentAdmin(Map<String, Object> claims) {
        Object role = claims == null ? null : claims.get("role");
        return "ROLE_CONTENT_ADMIN".equals(role) || "ROLE_SUPER_ADMIN".equals(role);
    }
}
