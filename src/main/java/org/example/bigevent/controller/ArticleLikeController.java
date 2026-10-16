package org.example.bigevent.controller;

import org.example.bigevent.pojo.ArticleLike;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.ArticleLikeService;
import org.example.bigevent.service.ArticleService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 文章点赞（legacy 兼容接口，前端 ArticleInteractions 仍在用）。
 *
 * <p>基础路径 {@code /article-like}；写操作需登录，用户 ID 取自 JWT claims。
 * 目标文章必须已发布，否则 404；返回体为 legacy 封套 {@code Result}。
 */
@RestController
@RequestMapping("/article-like")
public class ArticleLikeController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleLikeService articleLikeService;

    /**
     * 点赞文章。
     *
     * <p>{@code POST /article-like?articleId=}
     *
     * @param articleId 文章 ID（须已发布）
     * @return 空数据；文章未发布或不存在返回 404
     */
    @PostMapping
    public Result like(@RequestParam Integer articleId) {
        articleService.requirePublished(articleId);
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        articleLikeService.addLike(articleId, userId);
        return Result.success();
    }

    /**
     * 取消点赞。
     *
     * <p>{@code DELETE /article-like?articleId=}
     *
     * @param articleId 文章 ID（须已发布）
     * @return 空数据
     */
    @DeleteMapping
    public Result unlike(@RequestParam Integer articleId) {
        articleService.requirePublished(articleId);
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        articleLikeService.deleteLike(articleId, userId);
        return Result.success();
    }

    /**
     * 当前登录用户的点赞列表。
     *
     * <p>{@code GET /article-like/list}
     *
     * @return 点赞记录列表
     */
    @GetMapping("/list")
    public Result<List<ArticleLike>> listByUserId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        List<ArticleLike> likes = articleLikeService.listByUserId(userId);
        return Result.success(likes);
    }

    /**
     * 当前用户是否点赞过某文章。
     *
     * <p>{@code GET /article-like/check?articleId=}
     *
     * @param articleId 文章 ID（须已发布）
     * @return {@code true} 已点赞
     */
    @GetMapping("/check")
    public Result<Boolean> hasLiked(@RequestParam Integer articleId) {
        articleService.requirePublished(articleId);
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        Boolean hasLiked = articleLikeService.hasLiked(articleId, userId);
        return Result.success(hasLiked);
    }

    /**
     * 文章点赞数。
     *
     * <p>{@code GET /article-like/count?articleId=}
     *
     * @param articleId 文章 ID（须已发布）
     * @return 点赞总数
     */
    @GetMapping("/count")
    public Result<Integer> count(@RequestParam Integer articleId) {
        articleService.requirePublished(articleId);
        Integer count = articleLikeService.countByArticleId(articleId);
        return Result.success(count);
    }
}
