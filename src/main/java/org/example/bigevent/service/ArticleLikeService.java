package org.example.bigevent.service;

import org.example.bigevent.pojo.ArticleLike;

import java.util.List;

public interface ArticleLikeService {

    void addLike(Integer articleId, Integer userId);

    void deleteLike(Integer articleId, Integer userId);

    List<ArticleLike> listByUserId(Integer userId);

    Boolean hasLiked(Integer articleId, Integer userId);

    Integer countByArticleId(Integer articleId);
}
