package org.example.bigevent.pojo;

import java.time.LocalDateTime;

public class ArticleFavorite {
    private Integer id;//主键ID
    private Integer articleId;//文章ID
    private Integer userId;//用户ID
    private LocalDateTime createTime;//收藏时间

    public ArticleFavorite() {
    }

    public ArticleFavorite(Integer articleId, Integer userId) {
        this.articleId = articleId;
        this.userId = userId;
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
