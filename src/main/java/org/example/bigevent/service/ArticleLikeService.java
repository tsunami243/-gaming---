package org.example.bigevent.service;

import org.example.bigevent.pojo.ArticleLike;

import java.util.List;

/** 文章点赞：点赞、取消、查询与计数。 */
public interface ArticleLikeService {

    /**
     * 点赞（重复点赞按幂等处理）。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     */
    void addLike(Integer articleId, Integer userId);

    /**
     * 取消点赞（未点赞时按幂等处理）。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     */
    void deleteLike(Integer articleId, Integer userId);

    /**
     * 查询某用户点赞过的全部文章记录。
     *
     * @param userId 用户 ID
     * @return 点赞记录列表
     */
    List<ArticleLike> listByUserId(Integer userId);

    /**
     * 判断用户是否已点赞该文章。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 已点赞返回 {@code true}
     */
    Boolean hasLiked(Integer articleId, Integer userId);

    /**
     * 统计文章的点赞总数。
     *
     * @param articleId 文章 ID
     * @return 点赞数
     */
    Integer countByArticleId(Integer articleId);
}
