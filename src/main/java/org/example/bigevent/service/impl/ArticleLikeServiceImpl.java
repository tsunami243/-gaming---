package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.ArticleLikeMapper;
import org.example.bigevent.pojo.ArticleLike;
import org.example.bigevent.service.ArticleLikeService;
import org.example.bigevent.service.HeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArticleLikeServiceImpl implements ArticleLikeService {
    @Autowired
    private ArticleLikeMapper articleLikeMapper;
    @Autowired
    private HeatService heatService;

    @Override
    public void addLike(Integer articleId, Integer userId) {
        ArticleLike articleLike = new ArticleLike(articleId, userId);
        articleLike.setCreateTime(LocalDateTime.now());
        articleLikeMapper.add(articleLike);
        heatService.incr(articleId, HeatService.DELTA_LIKE); // 热度埋点：点赞 +2
    }

    @Override
    public void deleteLike(Integer articleId, Integer userId) {
        articleLikeMapper.delete(articleId, userId);
    }

    @Override
    public List<ArticleLike> listByUserId(Integer userId) {
        return articleLikeMapper.listByUserId(userId);
    }

    @Override
    public Boolean hasLiked(Integer articleId, Integer userId) {
        ArticleLike articleLike = articleLikeMapper.findByArticleIdAndUserId(articleId, userId);
        return articleLike != null;
    }

    @Override
    public Integer countByArticleId(Integer articleId) {
        return articleLikeMapper.countByArticleId(articleId);
    }
}
