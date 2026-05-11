package org.example.bigevent.service;

public interface ViewHistoryService {

    // 记录浏览（自动去重，24小时内同一用户/IP只记录一次）
    void recordView(Integer articleId, Integer userId, String ipAddress);

    // 获取文章浏览量
    Integer getViewCount(Integer articleId);

    // 获取文章独立浏览量（按用户数）
    Integer getUniqueViewCount(Integer articleId);
}
