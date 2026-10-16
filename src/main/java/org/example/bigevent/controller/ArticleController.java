package org.example.bigevent.controller;

import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.PageBean;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 文章（legacy 接口）。
 *
 * <p>基础路径 {@code /article}；读操作公开且只返回已发布内容，写操作需 CONTENT_ADMIN / SUPER_ADMIN。
 * 与 v1 公开读接口（{@code /api/v1/articles}）功能重叠，但返回 legacy {@code Article} 实体而非 DTO；
 * 新前端主阅读流已迁至 v1，此处主要保留给写路径与兼容调用。
 */
@RestController
@RequestMapping("/article")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    /**
     * 新增文章（需管理员）。
     *
     * <p>{@code POST /article}
     *
     * @param article 文章数据（@State 校验）
     * @return 空数据
     */
    @PostMapping
    public Result addArticle(@RequestBody @Validated Article article) {
        articleService.addArticle(article);
        return Result.success();
    }

    /**
     * 文章分页列表（公开）。
     *
     * <p>{@code GET /article}
     *
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @param categoryId 可选分类过滤
     * @param state      可选状态过滤
     * @return 分页文章（legacy PageBean）
     */
    @GetMapping
    public Result<PageBean<Article>> List(
            Integer pageNum,
            Integer pageSize,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String state) {
        PageBean<Article> articlePageBean = articleService.list(pageNum, pageSize, categoryId, state);
        return Result.success(articlePageBean);
    }

    /**
     * 按标签搜索文章（公开）。
     *
     * <p>{@code GET /article/search}
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param tag      标签关键字（可选）
     * @return 分页文章
     */
    @GetMapping("/search")
    public Result<PageBean<Article>> search(
            Integer pageNum,
            Integer pageSize,
            @RequestParam(required = false) String tag) {
        PageBean<Article> articlePageBean = articleService.search(pageNum, pageSize, tag);
        return Result.success(articlePageBean);
    }

    /**
     * 更新文章（需管理员）。
     *
     * <p>{@code PUT /article}
     *
     * @param article 文章数据（@State 校验）
     * @return 空数据
     */
    @PutMapping
    public Result update(@RequestBody @Validated Article article) {
        articleService.update(article);
        return Result.success();
    }

    /**
     * 文章详情（公开）。
     *
     * <p>{@code GET /article/detail?id=}
     *
     * @param id 文章 ID
     * @return 文章详情；不存在返回 404
     */
    @GetMapping("/detail")
    public Result<Article> detail(@RequestParam Integer id) {
        Article article = articleService.detail(id);
        if (article == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        }
        return Result.success(article);
    }

    /**
     * 删除文章（需管理员）。
     *
     * <p>{@code DELETE /article?id=}
     *
     * @param id 文章 ID
     * @return 空数据
     */
    @DeleteMapping
    public Result delete(@RequestParam Integer id) {
        articleService.delete(id);
        return Result.success();
    }

    /**
     * 轮播图文章列表（公开）。
     *
     * <p>{@code GET /article/carousel?limit=}
     *
     * @param limit 条数，<=0 或空时取 5
     * @return 轮播文章
     */
    @GetMapping("/carousel")
    public Result<List<Article>> getCarouselList(@RequestParam(required = false) Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 5;
        }
        List<Article> carouselList = articleService.getCarouselList(limit);
        return Result.success(carouselList);
    }

    /**
     * 文章统计信息（公开）。
     *
     * <p>{@code GET /article/stats?articleId=}
     *
     * @param articleId 文章 ID（须已发布）
     * @return 浏览/点赞/收藏/评论计数；文章不存在返回业务错误
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getArticleStats(@RequestParam Integer articleId) {
        articleService.requirePublished(articleId);
        Map<String, Object> stats = articleService.getArticleStats(articleId);
        if (stats != null) {
            return Result.success(stats);
        }
        return Result.error("文章不存在");
    }

    /**
     * 单篇热度分（公开）。
     *
     * <p>{@code GET /article/heat?articleId=}；公式 views*1 + uniqueViews*3 + favorites*5 + likes*2 + comments*3
     *
     * @param articleId 文章 ID（须已发布）
     * @return 热度分
     */
    @GetMapping("/heat")
    public Result<Integer> calculateHeat(@RequestParam Integer articleId) {
        articleService.requirePublished(articleId);
        Integer heat = articleService.calculateHeat(articleId);
        return Result.success(heat);
    }

    /**
     * 热度排行榜 TopN（公开）。
     *
     * <p>{@code GET /article/hot?limit=}
     *
     * @param limit 条数，<=0 或空时取 5
     * @return 按热度排序的文章
     */
    @GetMapping("/hot")
    public Result<List<Article>> hot(@RequestParam(defaultValue = "5") Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 5;
        }
        return Result.success(articleService.getHotList(limit));
    }
}
