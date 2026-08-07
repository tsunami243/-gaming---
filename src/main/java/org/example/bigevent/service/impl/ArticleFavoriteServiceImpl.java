package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.ArticleFavoriteMapper;
import org.example.bigevent.pojo.ArticleFavorite;
import org.example.bigevent.service.ArticleFavoriteService;
import org.example.bigevent.service.HeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArticleFavoriteServiceImpl implements ArticleFavoriteService {
    @Autowired
    private ArticleFavoriteMapper favoriteMapper;
    @Autowired
    private HeatService heatService;

    @Override
    public void addFavorite(Integer articleId, Integer userId) {
        // 检查是否已收藏
        if (hasFavorited(articleId, userId)) {
            return;
        }
        ArticleFavorite favorite = new ArticleFavorite(articleId, userId);
        favorite.setCreateTime(LocalDateTime.now());
        favoriteMapper.add(favorite);
        heatService.incr(articleId, HeatService.DELTA_FAVORITE); // 热度埋点：收藏 +5
    }

    @Override
    public void deleteFavorite(Integer articleId, Integer userId) {
        favoriteMapper.delete(articleId, userId);
    }

    @Override
    public Boolean hasFavorited(Integer articleId, Integer userId) {
        ArticleFavorite favorite = favoriteMapper.findByArticleIdAndUserId(articleId, userId);
        return favorite != null;
    }

    @Override
    public Integer getFavoriteCount(Integer articleId) {
        return favoriteMapper.countByArticleId(articleId);
    }

    @Override
    public List<ArticleFavorite> listByUserId(Integer userId) {
        return favoriteMapper.listByUserId(userId);
    }
}
