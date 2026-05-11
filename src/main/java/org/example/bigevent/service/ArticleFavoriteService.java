package org.example.bigevent.service;

import org.example.bigevent.pojo.ArticleFavorite;

import java.util.List;

public interface ArticleFavoriteService {

    // 添加收藏
    void addFavorite(Integer articleId, Integer userId);

    // 取消收藏
    void deleteFavorite(Integer articleId, Integer userId);

    // 检查是否已收藏
    Boolean hasFavorited(Integer articleId, Integer userId);

    // 获取收藏数
    Integer getFavoriteCount(Integer articleId);

    // 获取用户收藏列表
    List<ArticleFavorite> listByUserId(Integer userId);
}
