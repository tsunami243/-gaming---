package org.example.bigevent.service;

import org.example.bigevent.pojo.ArticleFavorite;

import java.util.List;

/** 文章收藏：收藏、取消、查询与计数。 */
public interface ArticleFavoriteService {

    /**
     * 收藏文章（重复收藏按幂等处理）。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     */
    void addFavorite(Integer articleId, Integer userId);

    /**
     * 取消收藏（未收藏时按幂等处理）。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     */
    void deleteFavorite(Integer articleId, Integer userId);

    /**
     * 检查用户是否已收藏该文章。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 已收藏返回 {@code true}
     */
    Boolean hasFavorited(Integer articleId, Integer userId);

    /**
     * 获取文章的收藏总数。
     *
     * @param articleId 文章 ID
     * @return 收藏数
     */
    Integer getFavoriteCount(Integer articleId);

    /**
     * 获取某用户的收藏记录列表。
     *
     * @param userId 用户 ID
     * @return 收藏记录列表
     */
    List<ArticleFavorite> listByUserId(Integer userId);
}
