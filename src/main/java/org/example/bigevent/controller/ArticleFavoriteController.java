package org.example.bigevent.controller;

import org.example.bigevent.pojo.ArticleFavorite;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.ArticleFavoriteService;
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
 * 文章收藏（legacy 兼容接口，前端 ArticleInteractions 仍在用）。
 *
 * <p>基础路径 {@code /article-favorite}；写操作需登录，用户 ID 取自 JWT claims。
 * 目标文章必须已发布，否则 404。分页版收藏列表见 {@code GET /api/v1/me/favorites}。
 */
@RestController
@RequestMapping("/article-favorite")
public class ArticleFavoriteController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleFavoriteService favoriteService;

    /**
     * 收藏文章。
     *
     * <p>{@code POST /article-favorite?articleId=}
     *
     * @param articleId 文章 ID（须已发布）
     * @return 空数据；文章未发布或不存在返回 404
     */
    @PostMapping
    public Result addFavorite(@RequestParam Integer articleId) {
        articleService.requirePublished(articleId);
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        favoriteService.addFavorite(articleId, userId);
        return Result.success();
    }

    /**
     * 取消收藏。
     *
     * <p>{@code DELETE /article-favorite?articleId=}
     *
     * @param articleId 文章 ID（须已发布）
     * @return 空数据
     */
    @DeleteMapping
    public Result deleteFavorite(@RequestParam Integer articleId) {
        articleService.requirePublished(articleId);
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        favoriteService.deleteFavorite(articleId, userId);
        return Result.success();
    }

    /**
     * 当前用户是否收藏过某文章。
     *
     * <p>{@code GET /article-favorite/check?articleId=}
     *
     * @param articleId 文章 ID（须已发布）
     * @return {@code true} 已收藏
     */
    @GetMapping("/check")
    public Result<Boolean> checkFavorite(@RequestParam Integer articleId) {
        articleService.requirePublished(articleId);
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        Boolean hasFavorited = favoriteService.hasFavorited(articleId, userId);
        return Result.success(hasFavorited);
    }

    /**
     * 文章收藏数。
     *
     * <p>{@code GET /article-favorite/count?articleId=}
     *
     * @param articleId 文章 ID（须已发布）
     * @return 收藏总数
     */
    @GetMapping("/count")
    public Result<Integer> getFavoriteCount(@RequestParam Integer articleId) {
        articleService.requirePublished(articleId);
        Integer count = favoriteService.getFavoriteCount(articleId);
        return Result.success(count);
    }

    /**
     * 当前登录用户的收藏列表（legacy 不分页版本）。
     *
     * <p>{@code GET /article-favorite/list}
     *
     * @return 收藏记录列表
     */
    @GetMapping("/list")
    public Result<List<ArticleFavorite>> listByUserId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        List<ArticleFavorite> favorites = favoriteService.listByUserId(userId);
        return Result.success(favorites);
    }
}
