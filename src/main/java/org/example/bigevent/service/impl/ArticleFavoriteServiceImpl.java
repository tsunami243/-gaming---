package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.ArticleFavoriteMapper;
import org.example.bigevent.pojo.ArticleFavorite;
import org.example.bigevent.service.ArticleFavoriteService;
import org.example.bigevent.service.HeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章收藏的业务实现。
 *
 * <p>收藏是幂等的：已收藏时直接返回，不重复写库，也不重复累加热度。
 * 与点赞一致，取消收藏不回退热度。
 */
@Service
public class ArticleFavoriteServiceImpl implements ArticleFavoriteService {

    @Autowired
    private ArticleFavoriteMapper favoriteMapper;
    @Autowired
    private HeatService heatService;

    /**
     * 收藏文章；已收藏时不产生任何副作用。
     *
     * @param articleId 文章 ID
     * @param userId    收藏用户 ID
     */
    @Override
    public void addFavorite(Integer articleId, Integer userId) {
        // 已收藏则直接返回，保证幂等
        if (hasFavorited(articleId, userId)) {
            return;
        }
        ArticleFavorite favorite = new ArticleFavorite(articleId, userId);
        favorite.setCreateTime(LocalDateTime.now());
        favoriteMapper.add(favorite);
        heatService.incr(articleId, HeatService.DELTA_FAVORITE); // 热度埋点：收藏 +5
    }

    /**
     * 取消收藏。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     */
    @Override
    public void deleteFavorite(Integer articleId, Integer userId) {
        favoriteMapper.delete(articleId, userId);
    }

    /**
     * 判断用户是否已收藏该文章。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 已收藏返回 {@code true}
     */
    @Override
    public Boolean hasFavorited(Integer articleId, Integer userId) {
        ArticleFavorite favorite = favoriteMapper.findByArticleIdAndUserId(articleId, userId);
        return favorite != null;
    }

    /**
     * 统计某篇文章的收藏数。
     *
     * @param articleId 文章 ID
     * @return 收藏总数
     */
    @Override
    public Integer getFavoriteCount(Integer articleId) {
        return favoriteMapper.countByArticleId(articleId);
    }

    /**
     * 查询某个用户的收藏列表。
     *
     * @param userId 用户 ID
     * @return 收藏记录列表
     */
    @Override
    public List<ArticleFavorite> listByUserId(Integer userId) {
        return favoriteMapper.listByUserId(userId);
    }
}
