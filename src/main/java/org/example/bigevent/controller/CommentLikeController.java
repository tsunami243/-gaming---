package org.example.bigevent.controller;

import org.example.bigevent.pojo.CommentLike;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.CommentLikeService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comment-like")
public class CommentLikeController {
    @Autowired
    private CommentLikeService commentLikeService;

    //点赞评论
    @PostMapping
    public Result like(@RequestParam Integer commentId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        commentLikeService.addLike(commentId, userId);
        return Result.success();
    }

    //取消点赞
    @DeleteMapping
    public Result unlike(@RequestParam Integer commentId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        commentLikeService.deleteLike(commentId, userId);
        return Result.success();
    }

    //获取用户的点赞列表
    @GetMapping("/list")
    public Result<List<CommentLike>> listByUserId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        List<CommentLike> likes = commentLikeService.listByUserId(userId);
        return Result.success(likes);
    }

    //检查用户是否点赞过某评论
    @GetMapping("/check")
    public Result<Boolean> hasLiked(@RequestParam Integer commentId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        Boolean hasLiked = commentLikeService.hasLiked(commentId, userId);
        return Result.success(hasLiked);
    }

    //获取评论点赞数
    @GetMapping("/count")
    public Result<Integer> count(@RequestParam Integer commentId) {
        Integer count = commentLikeService.countByCommentId(commentId);
        return Result.success(count);
    }
}
