package org.example.bigevent.pojo;

import java.time.LocalDateTime;

/** 评论点赞记录：记录「哪个用户点赞了哪条评论」及点赞时间。 */
public class CommentLike {
    private Integer id;//主键ID
    private Integer commentId;//评论ID
    private Integer userId;//用户ID
    private LocalDateTime createTime;//点赞时间

    public CommentLike() {
    }

    public CommentLike(Integer commentId, Integer userId) {
        this.commentId = commentId;
        this.userId = userId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCommentId() {
        return commentId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
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
