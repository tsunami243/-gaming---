package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.ArticleLikeMapper;
import org.example.bigevent.pojo.ArticleLike;
import org.example.bigevent.service.ArticleLikeService;
import org.example.bigevent.service.HeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章点赞的业务实现。
 *
 * <p>点赞成功后会同步累加热度（{@link HeatService#DELTA_LIKE}）；取消点赞不回退热度，
 * 热度是单向累加的近似指标，不作为账目使用。
 */
@Service
public class ArticleLikeServiceImpl implements ArticleLikeService {

    @Autowired
    private ArticleLikeMapper articleLikeMapper;
    @Autowired
    private HeatService heatService;

    /**
     * 新增一条文章点赞记录，并累加该文章热度。
     *
     * @param articleId 文章 ID
     * @param userId    点赞用户 ID
     */
    @Override
    public void addLike(Integer articleId, Integer userId) {
        ArticleLike articleLike = new ArticleLike(articleId, userId);
        articleLike.setCreateTime(LocalDateTime.now());
        articleLikeMapper.add(articleLike);
        heatService.incr(articleId, HeatService.DELTA_LIKE); // 热度埋点：点赞 +2
    }

    /**
     * 取消文章点赞（不回退热度）。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     */
    @Override
    public void deleteLike(Integer articleId, Integer userId) {
        articleLikeMapper.delete(articleId, userId);
    }

    /**
     * 查询某个用户点赞过的全部文章。
     *
     * @param userId 用户 ID
     * @return 点赞记录列表
     */
    @Override
    public List<ArticleLike> listByUserId(Integer userId) {
        return articleLikeMapper.listByUserId(userId);
    }

    /**
     * 判断用户是否已点赞该文章。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 已点赞返回 {@code true}
     */
    @Override
    public Boolean hasLiked(Integer articleId, Integer userId) {
        ArticleLike articleLike = articleLikeMapper.findByArticleIdAndUserId(articleId, userId);
        return articleLike != null;
    }

    /**
     * 统计某篇文章的点赞数。
     *
     * @param articleId 文章 ID
     * @return 点赞总数
     */
    @Override
    public Integer countByArticleId(Integer articleId) {
        return articleLikeMapper.countByArticleId(articleId);
    }
}
