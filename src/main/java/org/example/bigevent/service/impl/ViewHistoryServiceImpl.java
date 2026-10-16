package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.ArticleViewHistoryMapper;
import org.example.bigevent.pojo.ArticleViewHistory;
import org.example.bigevent.service.HeatService;
import org.example.bigevent.service.ViewHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 文章浏览记录的业务实现。
 *
 * <p>一次浏览同时产生两种效果：
 * <ol>
 *   <li>热度埋点——通过 Redis Set 判重，首次独立浏览与重复浏览的增量不同；</li>
 *   <li>明细落库——同一用户或同一 IP 在 24 小时内只记录一条。</li>
 * </ol>
 * 热度埋点在校验之前执行，因此重复浏览同样会累加热度（只是增量更小）。
 */
@Service
public class ViewHistoryServiceImpl implements ViewHistoryService {

    @Autowired
    private ArticleViewHistoryMapper viewHistoryMapper;
    @Autowired
    private HeatService heatService;

    /**
     * 记录一次文章浏览。
     *
     * <p>登录用户以 {@code u{userId}} 作为热度去重标识，游客以 {@code ip:{地址}} 作为标识；
     * 明细落库前会检查 24 小时内是否已有同一用户或 IP 的记录，命中则不再写库。
     *
     * @param articleId 文章 ID
     * @param userId    浏览用户 ID；游客为 {@code null}
     * @param ipAddress 浏览来源 IP；登录用户可不传
     */
    @Override
    public void recordView(Integer articleId, Integer userId, String ipAddress) {
        // 热度埋点：Redis Set 判重（首次独立浏览 +4，重复 +1），MySQL 落库逻辑保持不变
        String member = userId != null ? "u" + userId : "ip:" + (ipAddress == null ? "unknown" : ipAddress);
        heatService.recordView(articleId, member);

        // 明细落库：24 小时内同一用户/IP 只记一次
        if (userId != null) {
            if (viewHistoryMapper.findRecentByUserId(articleId, userId) != null) {
                return;
            }
        } else if (ipAddress != null) {
            if (viewHistoryMapper.findRecentByIpAddress(articleId, ipAddress) != null) {
                return;
            }
        }

        ArticleViewHistory viewHistory = new ArticleViewHistory(articleId, userId, ipAddress);
        viewHistory.setViewTime(LocalDateTime.now());
        viewHistoryMapper.add(viewHistory);
    }

    /**
     * 统计文章的总浏览数（含重复浏览）。
     *
     * @param articleId 文章 ID
     * @return 浏览明细总条数
     */
    @Override
    public Integer getViewCount(Integer articleId) {
        return viewHistoryMapper.countByArticleId(articleId);
    }

    /**
     * 统计文章的独立浏览数。
     *
     * @param articleId 文章 ID
     * @return 按用户去重后的浏览数
     */
    @Override
    public Integer getUniqueViewCount(Integer articleId) {
        return viewHistoryMapper.countUniqueViewsByUserId(articleId);
    }
}
