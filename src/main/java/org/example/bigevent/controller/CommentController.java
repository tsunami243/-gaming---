package org.example.bigevent.controller;

import org.example.bigevent.pojo.Comment;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.CommentService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comment")
public class CommentController {
    @Autowired
    private CommentService commentService;

    //添加评论（直接回复文章）
    @PostMapping
    public Result addComment(@RequestBody @Validated Comment comment) {
        Map<String, Object> map = ThreadLocalUtil.get();
        comment.setUserId((Integer) map.get("userId"));
        comment.setUserName((String) map.get("username"));
        comment.setUserAvatar("");
        commentService.addComment(comment);
        return Result.success();
    }

    //回复评论
    @PostMapping("/{articleId}/reply")
    public Result replyComment(
            @PathVariable Integer articleId,
            @RequestParam Integer parentId,
            @RequestParam(required = false) Integer replyId,
            @RequestParam String content) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId((Integer) map.get("userId"));
        comment.setUserName((String) map.get("username"));
        comment.setUserAvatar("");
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setReplyId(replyId);
        // 如果是回复某条评论，设置被回复用户名
        if (replyId != null) {
            Comment repliedComment = commentService.detail(replyId);
            if (repliedComment != null) {
                comment.setReplyUserName(repliedComment.getUserName());
            }
        }
        commentService.addComment(comment);
        return Result.success();
    }

    //获取文章的所有评论（扁平列表）
    @GetMapping("/{articleId}")
    public Result<List<Comment>> listByArticleId(@PathVariable Integer articleId) {
        List<Comment> comments = commentService.listByArticleId(articleId);
        return Result.success(comments);
    }

    //获取文章的评论树（顶级评论+回复）
    @GetMapping("/{articleId}/tree")
    public Result<List<Comment>> listArticleCommentTree(@PathVariable Integer articleId) {
        List<Comment> comments = commentService.listArticleCommentTree(articleId);
        return Result.success(comments);
    }

    //删除评论
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        commentService.delete(id, userId);
        return Result.success();
    }

    //更新评论点赞数
    @PutMapping("/{id}/like")
    public Result updateLikeCount(@PathVariable Integer id, @RequestParam Integer likeCount) {
        commentService.updateLikeCount(id, likeCount);
        return Result.success();
    }
}
