package org.example.bigevent.pojo;

import java.time.LocalDateTime;

public class ArticleViewHistory {
    private Integer id;//主键ID
    private Integer articleId;//文章ID
    private Integer userId;//用户ID，游客为null
    private String ipAddress;//IP地址，用于游客去重
    private LocalDateTime viewTime;//浏览时间

    public ArticleViewHistory() {
    }

    public ArticleViewHistory(Integer articleId, Integer userId, String ipAddress) {
        this.articleId = articleId;
        this.userId = userId;
        this.ipAddress = ipAddress;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getArticleId() {
        return articleId;
    }

    public void setArticleId(Integer articleId) {
        this.articleId = articleId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getViewTime() {
        return viewTime;
    }

    public void setViewTime(LocalDateTime viewTime) {
        this.viewTime = viewTime;
    }
}
