package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.ArticleViewHistoryMapper;
import org.example.bigevent.pojo.ArticleViewHistory;
import org.example.bigevent.service.ViewHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ViewHistoryServiceImpl implements ViewHistoryService {
    @Autowired
    private ArticleViewHistoryMapper viewHistoryMapper;

    @Override
    public void recordView(Integer articleId, Integer userId, String ipAddress) {
        // 检查是否重复浏览（24小时内同一用户/IP只算一次）
        if (userId != null) {
            // 登录用户，按userId检查
            ArticleViewHistory recent = viewHistoryMapper.findRecentByUserId(articleId, userId);
            if (recent != null) {
                // 24小时内已浏览过，不重复记录
                return;
            }
        } else if (ipAddress != null) {
            // 游客，按IP地址检查
            ArticleViewHistory recent = viewHistoryMapper.findRecentByIpAddress(articleId, ipAddress);
            if (recent != null) {
                // 24小时内已浏览过，不重复记录
                return;
            }
        }

        // 记录浏览
        ArticleViewHistory viewHistory = new ArticleViewHistory(articleId, userId, ipAddress);
        viewHistory.setViewTime(LocalDateTime.now());
        viewHistoryMapper.add(viewHistory);
    }

    @Override
    public Integer getViewCount(Integer articleId) {
        return viewHistoryMapper.countByArticleId(articleId);
    }

    @Override
    public Integer getUniqueViewCount(Integer articleId) {
        return viewHistoryMapper.countUniqueViewsByUserId(articleId);
    }
}
