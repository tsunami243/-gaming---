package org.example.bigevent.controller;

import org.example.bigevent.pojo.ArticleLike;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.ArticleLikeService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/article-like")
public class ArticleLikeController {
    @Autowired
    private ArticleLikeService articleLikeService;

    //点赞文章
    @PostMapping
    public Result like(@RequestParam Integer articleId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        articleLikeService.addLike(articleId, userId);
        return Result.success();
    }

    //取消点赞
    @DeleteMapping
    public Result unlike(@RequestParam Integer articleId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        articleLikeService.deleteLike(articleId, userId);
        return Result.success();
    }

    //获取用户的点赞列表
    @GetMapping("/list")
    public Result<List<ArticleLike>> listByUserId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        List<ArticleLike> likes = articleLikeService.listByUserId(userId);
        return Result.success(likes);
    }

    //检查用户是否点赞过某文章
    @GetMapping("/check")
    public Result<Boolean> hasLiked(@RequestParam Integer articleId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        Boolean hasLiked = articleLikeService.hasLiked(articleId, userId);
        return Result.success(hasLiked);
    }

    //获取文章点赞数
    @GetMapping("/count")
    public Result<Integer> count(@RequestParam Integer articleId) {
        Integer count = articleLikeService.countByArticleId(articleId);
        return Result.success(count);
    }
}
