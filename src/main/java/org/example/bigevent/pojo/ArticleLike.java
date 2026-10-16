package org.example.bigevent.pojo;

import java.time.LocalDateTime;

/** 文章点赞记录：记录「哪个用户点赞了哪篇文章」及点赞时间。 */
public class ArticleLike {
    private Integer id;//主键ID
    private Integer articleId;//文章ID
    private Integer userId;//用户ID
    private LocalDateTime createTime;//点赞时间

    public ArticleLike() {
    }

    public ArticleLike(Integer articleId, Integer userId) {
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
