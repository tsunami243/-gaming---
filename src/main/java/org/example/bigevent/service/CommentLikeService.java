package org.example.bigevent.service;

import org.example.bigevent.pojo.CommentLike;

import java.util.List;

/** 评论点赞：点赞、取消、查询与计数。 */
public interface CommentLikeService {

    /**
     * 点赞评论（重复点赞按幂等处理）。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     */
    void addLike(Integer commentId, Integer userId);

    /**
     * 取消点赞（未点赞时按幂等处理）。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     */
    void deleteLike(Integer commentId, Integer userId);

    /**
     * 查询某用户点赞过的全部评论记录。
     *
     * @param userId 用户 ID
     * @return 点赞记录列表
     */
    List<CommentLike> listByUserId(Integer userId);

    /**
     * 判断用户是否已点赞该评论。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     * @return 已点赞返回 {@code true}
     */
    Boolean hasLiked(Integer commentId, Integer userId);

    /**
     * 统计评论的点赞总数。
     *
     * @param commentId 评论 ID
     * @return 点赞数
     */
    Integer countByCommentId(Integer commentId);
}
