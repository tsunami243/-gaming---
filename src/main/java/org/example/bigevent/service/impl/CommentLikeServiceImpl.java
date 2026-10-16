package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.CommentLikeMapper;
import org.example.bigevent.pojo.CommentLike;
import org.example.bigevent.service.CommentLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论点赞的业务实现。
 *
 * <p>只维护点赞关系本身（写入点赞时间、按条件查询、计数），不参与热度计算，
 * 也不校验评论是否可见——可见性由调用方的评论服务负责。
 */
@Service
public class CommentLikeServiceImpl implements CommentLikeService {

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    /**
     * 新增一条评论点赞记录。
     *
     * @param commentId 评论 ID
     * @param userId    点赞用户 ID
     */
    @Override
    public void addLike(Integer commentId, Integer userId) {
        CommentLike commentLike = new CommentLike(commentId, userId);
        commentLike.setCreateTime(LocalDateTime.now());
        commentLikeMapper.add(commentLike);
    }

    /**
     * 取消评论点赞。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     */
    @Override
    public void deleteLike(Integer commentId, Integer userId) {
        commentLikeMapper.delete(commentId, userId);
    }

    /**
     * 查询某个用户点赞过的全部评论。
     *
     * @param userId 用户 ID
     * @return 点赞记录列表
     */
    @Override
    public List<CommentLike> listByUserId(Integer userId) {
        return commentLikeMapper.listByUserId(userId);
    }

    /**
     * 判断用户是否已点赞该评论。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     * @return 已点赞返回 {@code true}
     */
    @Override
    public Boolean hasLiked(Integer commentId, Integer userId) {
        CommentLike commentLike = commentLikeMapper.findByCommentIdAndUserId(commentId, userId);
        return commentLike != null;
    }

    /**
     * 统计某条评论的点赞数。
     *
     * @param commentId 评论 ID
     * @return 点赞总数
     */
    @Override
    public Integer countByCommentId(Integer commentId) {
        return commentLikeMapper.countByCommentId(commentId);
    }
}
