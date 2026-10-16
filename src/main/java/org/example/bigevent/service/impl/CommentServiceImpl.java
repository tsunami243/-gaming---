package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.CommentMapper;
import org.example.bigevent.pojo.Comment;
import org.example.bigevent.service.CommentService;
import org.example.bigevent.service.HeatService;
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
 * 评论业务实现：负责评论的校验、落库与两级评论树组装。
 *
 * <p>评论以扁平结构存储（{@code parent_id} 指向顶级评论、{@code reply_id} 指向被回复者），
 * 读取时在内存中按 {@code parentId} 分组还原成「顶级评论 + 其回复列表」的树形结构。</p>
 */
@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private HeatService heatService;

    /**
     * 新增评论：校验字段合法性，并保证父评论/回复对象属于同一篇文章。
     *
     * @param comment 待新增的评论（需携带 articleId、userId 与正文）
     * @throws ResponseStatusException 字段缺失、正文超长或父子文章不一致时返回 400
     */
    @Override
    public void addComment(Comment comment) {
        validate(comment);
        requireSameArticle(comment, comment.getParentId(), "父评论不属于当前文章");
        requireSameArticle(comment, comment.getReplyId(), "回复对象不属于当前文章");

        LocalDateTime now = LocalDateTime.now();
        comment.setCreateTime(now);
        comment.setUpdateTime(now);
        commentMapper.add(comment);

        // 热度埋点：新增评论 +3
        heatService.incr(comment.getArticleId(), HeatService.DELTA_COMMENT);
    }

    /**
     * 查询某篇文章下的全部评论（扁平列表）。
     *
     * @param articleId 文章 ID
     * @return 该文章的评论列表，无数据时为空列表
     */
    @Override
    public List<Comment> listByArticleId(Integer articleId) {
        return commentMapper.listByArticleId(articleId);
    }

    /**
     * 查询某篇文章的评论树：顶级评论 + 各自挂载的回复列表。
     *
     * @param articleId 文章 ID
     * @return 顶级评论列表，每条顶级评论的 {@code replies} 均已填充（无回复时为空列表）
     */
    @Override
    public List<Comment> listArticleCommentTree(Integer articleId) {
        // 顶级评论（parent_id 为 null）
        List<Comment> topLevelComments = commentMapper.listTopLevelByArticleId(articleId);

        // 全部评论，用于挑出 parent_id 非空者作为回复
        List<Comment> allReplies = commentMapper.listByArticleId(articleId);

        Map<Integer, List<Comment>> replyMap = new HashMap<>();
        for (Comment reply : allReplies) {
            if (reply.getParentId() != null) {
                replyMap.computeIfAbsent(reply.getParentId(), key -> new ArrayList<>()).add(reply);
            }
        }

        for (Comment topComment : topLevelComments) {
            List<Comment> replies = replyMap.get(topComment.getId());
            topComment.setReplies(replies != null ? replies : new ArrayList<>());
        }

        return topLevelComments;
    }

    /**
     * 查询单条评论详情。
     *
     * @param id 评论 ID
     * @return 评论实体，不存在时返回 {@code null}
     */
    @Override
    public Comment detail(Integer id) {
        return commentMapper.detail(id);
    }

    /**
     * 删除评论：仅允许作者本人删除，删除条件由 Mapper 以 {@code userId} 限定。
     *
     * @param id     评论 ID
     * @param userId 当前登录用户 ID
     */
    @Override
    public void delete(Integer id, Integer userId) {
        commentMapper.delete(id, userId);
    }

    /**
     * 更新评论点赞数（由点赞服务调用）。
     *
     * @param id        评论 ID
     * @param likeCount 目标点赞数
     */
    @Override
    public void updateLikeCount(Integer id, Integer likeCount) {
        commentMapper.updateLikeCount(id, likeCount);
    }

    /**
     * 校验评论必填字段：文章、用户必须有效，正文 1~1000 字符。
     *
     * @param comment 待校验评论
     * @throws ResponseStatusException 任一项不满足时返回 400
     */
    private void validate(Comment comment) {
        boolean invalid = comment == null
                || comment.getArticleId() == null || comment.getArticleId() < 1
                || comment.getUserId() == null || comment.getUserId() < 1
                || comment.getContent() == null || comment.getContent().isBlank()
                || comment.getContent().length() > 1000;
        if (invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评论需包含有效文章、用户及1至1000字符正文");
        }
    }

    /**
     * 校验被引用的评论确实属于同一篇文章，防止跨文章挂载。
     *
     * @param comment   当前评论
     * @param targetId  被引用评论 ID（parentId 或 replyId），为 null 时跳过校验
     * @param message   校验失败时的错误提示
     * @throws ResponseStatusException 引用不存在或文章不一致时返回 400
     */
    private void requireSameArticle(Comment comment, Integer targetId, String message) {
        if (targetId == null) {
            return;
        }
        Comment target = commentMapper.detail(targetId);
        if (target == null || !comment.getArticleId().equals(target.getArticleId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }
}
