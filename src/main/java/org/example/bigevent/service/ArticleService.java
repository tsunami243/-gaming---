package org.example.bigevent.service;

import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.PageBean;

import java.util.List;
import java.util.Map;

public interface ArticleService {

    void addArticle(Article article);

    void addArticle(Article article, Integer createUser);

    PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state);

    void update(Article article);

    Article detail(Integer id);

    void delete(Integer id);

    List<Article> getCarouselList(Integer limit);

    Map<String, Object> getArticleStats(Integer articleId);

    Integer calculateHeat(Integer articleId);
}
