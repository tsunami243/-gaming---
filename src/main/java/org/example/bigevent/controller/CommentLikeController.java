package org.example.bigevent.controller;

import org.example.bigevent.pojo.Comment;
import org.example.bigevent.pojo.CommentLike;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.ArticleService;
import org.example.bigevent.service.CommentLikeService;
import org.example.bigevent.service.CommentService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 评论点赞（legacy 接口）。
 *
 * <p>基础路径 {@code /comment-like}；写操作需登录。
 * 目标评论必须存在且其所属文章已发布，否则 404。
 */
@RestController
@RequestMapping("/comment-like")
public class CommentLikeController {

    @Autowired
    private CommentLikeService commentLikeService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ArticleService articleService;

    /**
     * 点赞评论。
     *
     * <p>{@code POST /comment-like?commentId=}
     *
     * @param commentId 评论 ID（须存在且所属文章已发布）
     * @return 空数据
     */
    @PostMapping
    public Result like(@RequestParam Integer commentId) {
        requirePublishedComment(commentId);
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        commentLikeService.addLike(commentId, userId);
        return Result.success();
    }

    /**
     * 取消点赞。
     *
     * <p>{@code DELETE /comment-like?commentId=}
     *
     * @param commentId 评论 ID
     * @return 空数据
     */
    @DeleteMapping
    public Result unlike(@RequestParam Integer commentId) {
        requirePublishedComment(commentId);
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        commentLikeService.deleteLike(commentId, userId);
        return Result.success();
    }

    /**
     * 当前登录用户的评论点赞列表。
     *
     * <p>{@code GET /comment-like/list}
     *
     * @return 点赞记录列表
     */
    @GetMapping("/list")
    public Result<List<CommentLike>> listByUserId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        List<CommentLike> likes = commentLikeService.listByUserId(userId);
        return Result.success(likes);
    }

    /**
     * 当前用户是否点赞过某评论。
     *
     * <p>{@code GET /comment-like/check?commentId=}
     *
     * @param commentId 评论 ID
     * @return {@code true} 已点赞
     */
    @GetMapping("/check")
    public Result<Boolean> hasLiked(@RequestParam Integer commentId) {
        requirePublishedComment(commentId);
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        Boolean hasLiked = commentLikeService.hasLiked(commentId, userId);
        return Result.success(hasLiked);
    }

    /**
     * 评论点赞数。
     *
     * <p>{@code GET /comment-like/count?commentId=}
     *
     * @param commentId 评论 ID
     * @return 点赞总数
     */
    @GetMapping("/count")
    public Result<Integer> count(@RequestParam Integer commentId) {
        requirePublishedComment(commentId);
        Integer count = commentLikeService.countByCommentId(commentId);
        return Result.success(count);
    }

    /** 校验评论存在且其所属文章已发布，否则 404。 */
    private void requirePublishedComment(Integer commentId) {
        Comment comment = commentService.detail(commentId);
        if (comment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在");
        }
        articleService.requirePublished(comment.getArticleId());
    }
}
