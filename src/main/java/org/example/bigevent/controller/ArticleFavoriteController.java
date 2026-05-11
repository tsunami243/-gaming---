package org.example.bigevent.controller;

import org.example.bigevent.pojo.ArticleFavorite;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.ArticleFavoriteService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/article-favorite")
public class ArticleFavoriteController {
    @Autowired
    private ArticleFavoriteService favoriteService;

    // 收藏文章
    @PostMapping
    public Result addFavorite(@RequestParam Integer articleId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        favoriteService.addFavorite(articleId, userId);
        return Result.success();
    }

    // 取消收藏
    @DeleteMapping
    public Result deleteFavorite(@RequestParam Integer articleId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        favoriteService.deleteFavorite(articleId, userId);
        return Result.success();
    }

    // 检查是否收藏
    @GetMapping("/check")
    public Result<Boolean> checkFavorite(@RequestParam Integer articleId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        Boolean hasFavorited = favoriteService.hasFavorited(articleId, userId);
        return Result.success(hasFavorited);
    }

    // 获取收藏数
    @GetMapping("/count")
    public Result<Integer> getFavoriteCount(@RequestParam Integer articleId) {
        Integer count = favoriteService.getFavoriteCount(articleId);
        return Result.success(count);
    }

    // 获取用户收藏列表
    @GetMapping("/list")
    public Result<List<ArticleFavorite>> listByUserId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        List<ArticleFavorite> favorites = favoriteService.listByUserId(userId);
        return Result.success(favorites);
    }
}
