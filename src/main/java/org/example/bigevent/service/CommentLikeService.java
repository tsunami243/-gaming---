package org.example.bigevent.service;

import org.example.bigevent.pojo.CommentLike;

import java.util.List;

public interface CommentLikeService {

    void addLike(Integer commentId, Integer userId);

    void deleteLike(Integer commentId, Integer userId);

    List<CommentLike> listByUserId(Integer userId);

    Boolean hasLiked(Integer commentId, Integer userId);

    Integer countByCommentId(Integer commentId);
}
