package org.example.bigevent.service;

import org.example.bigevent.pojo.Comment;

import java.util.List;

public interface CommentService {

    void addComment(Comment comment);

    List<Comment> listByArticleId(Integer articleId);

    List<Comment> listArticleCommentTree(Integer articleId);

    Comment detail(Integer id);

    void delete(Integer id, Integer userId);

    void updateLikeCount(Integer id, Integer likeCount);
}
