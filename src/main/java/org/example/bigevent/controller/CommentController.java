package org.example.bigevent.controller;

import org.example.bigevent.pojo.Comment;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.ArticleService;
import org.example.bigevent.service.CommentService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * 评论（legacy 接口）。
 *
 * <p>基础路径 {@code /comment}；读操作公开（仅已发布文章），写操作需登录。
 * <b>提交口默认关闭</b>：由配置 {@code comments.submission-enabled} 控制，
 * 关闭时发评论/回复返回 503（举报、删除处置与限流验收通过前不开放）。
 */
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Value("${comments.submission-enabled:false}")
    private boolean submissionEnabled;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private CommentService commentService;

    /**
     * 发表评论（直接回复文章）。
     *
     * <p>{@code POST /comment}
     *
     * @param comment 评论体（含 articleId 与正文）
     * @return 空数据；提交口关闭返回 503；文章未发布返回 404
     */
    @PostMapping
    public Result addComment(@RequestBody @Validated Comment comment) {
        requireSubmissionEnabled();
        articleService.requirePublished(comment.getArticleId());
        Map<String, Object> map = ThreadLocalUtil.get();
        comment.setUserId((Integer) map.get("userId"));
        comment.setUserName((String) map.get("username"));
        comment.setUserAvatar("");
        commentService.addComment(comment);
        return Result.success();
    }

    /**
     * 回复某条评论。
     *
     * <p>{@code POST /comment/{articleId}/reply}
     *
     * @param articleId 文章 ID
     * @param parentId  顶级评论 ID
     * @param replyId   被回复的评论 ID（可选）
     * @param content   回复正文
     * @return 空数据；提交口关闭返回 503
     */
    @PostMapping("/{articleId}/reply")
    public Result replyComment(
            @PathVariable Integer articleId,
            @RequestParam Integer parentId,
            @RequestParam(required = false) Integer replyId,
            @RequestParam String content) {
        requireSubmissionEnabled();
        articleService.requirePublished(articleId);
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

    /**
     * 文章评论（扁平列表）。
     *
     * <p>{@code GET /comment/{articleId}}
     *
     * @param articleId 文章 ID（须已发布）
     * @return 评论列表
     */
    @GetMapping("/{articleId}")
    public Result<List<Comment>> listByArticleId(@PathVariable Integer articleId) {
        articleService.requirePublished(articleId);
        List<Comment> comments = commentService.listByArticleId(articleId);
        return Result.success(comments);
    }

    /**
     * 文章评论树（顶级评论 + 回复）。
     *
     * <p>{@code GET /comment/{articleId}/tree}
     *
     * @param articleId 文章 ID（须已发布）
     * @return 树形评论列表
     */
    @GetMapping("/{articleId}/tree")
    public Result<List<Comment>> listArticleCommentTree(@PathVariable Integer articleId) {
        articleService.requirePublished(articleId);
        List<Comment> comments = commentService.listArticleCommentTree(articleId);
        return Result.success(comments);
    }

    /**
     * 删除评论（仅评论作者本人）。
     *
     * <p>{@code DELETE /comment/{id}}
     *
     * @param id 评论 ID
     * @return 空数据；评论不存在或其文章未发布返回 404
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        requirePublishedComment(id);
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        commentService.delete(id, userId);
        return Result.success();
    }

    /**
     * 直接更新评论点赞数（legacy）。
     *
     * <p>{@code PUT /comment/{id}/like}；安全层已对该动词 {@code denyAll}，实际不可达，
     * 保留仅为兼容说明，正常点赞走 {@code /comment-like}。
     *
     * @param id        评论 ID
     * @param likeCount 目标点赞数
     * @return 空数据
     */
    @PutMapping("/{id}/like")
    public Result updateLikeCount(@PathVariable Integer id, @RequestParam Integer likeCount) {
        requirePublishedComment(id);
        commentService.updateLikeCount(id, likeCount);
        return Result.success();
    }

    /** 提交口关闭时抛出 503。 */
    private void requireSubmissionEnabled() {
        if (!submissionEnabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "评论提交暂未开放");
        }
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
