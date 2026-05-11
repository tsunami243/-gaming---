package org.example.bigevent.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.example.bigevent.mapper.ArticleLikeMapper;
import org.example.bigevent.mapper.ArticleMapper;
import org.example.bigevent.mapper.CommentMapper;
import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.PageBean;
import org.example.bigevent.service.ArticleFavoriteService;
import org.example.bigevent.service.ArticleService;
import org.example.bigevent.service.ViewHistoryService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ArticleServicelmpl implements ArticleService {
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private ArticleLikeMapper articleLikeMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private ViewHistoryService viewHistoryService;
    @Autowired
    private ArticleFavoriteService articleFavoriteService;

    @Override
    public void addArticle(Article article) {
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        Map<String,Object> map= ThreadLocalUtil.get();
        article.setCreateUser((Integer) map.get("userId"));
        articleMapper.add(article);
    }

    @Override
    public PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state) {
        //创建pageBean对象
        PageBean<Article> pageBean = new PageBean<>();

        //开启分页查询pageHelper
        //他会将查询结果封装到Page对象中，还会将传入的参数拼接在sql语句中，实现动态sql查询
        PageHelper.startPage(pageNum, pageSize);
        //从ThreadLocal中获取用户id
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId=(Integer)map.get("userId");
        //调用mapper查询方法
        List<Article> list = articleMapper.list(categoryId,state,userId);
        if (list instanceof Page) {
            Page<Article> page = (Page<Article>) list;
            pageBean.setTotal(page.getTotal());
            pageBean.setItems(page.getResult());
        } else {
            // 如果不是Page类型，手动设置
            pageBean.setTotal((long) list.size());
            pageBean.setItems(list);
        }

        return pageBean;
    }

    @Override
    public void update(Article article) {
        article.setUpdateTime(LocalDateTime.now());
        //从ThreadLocal中获取用户id
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId=(Integer)map.get("userId");
        article.setCreateUser(userId);
        articleMapper.update(article);
    }

    @Override
    public Article detail(Integer id) {
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId=(Integer)map.get("userId");
        //调用mapper查询方法
        Article article = articleMapper.detail(id,userId);
        return article;
    }

    @Override
    public void delete(Integer id) {

        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId=(Integer)map.get("userId");
        //调用mapper查询方法
        articleMapper.delete(id,userId);
    }

    @Override
    public List<Article> getCarouselList(Integer limit) {

        return articleMapper.selectCarouselList(limit);
    }

    //统计文章的浏览量、独立浏览量、收藏量、点赞量、评论量（使用独立统计表）
    @Override
    public Map<String, Object> getArticleStats(Integer articleId) {
        Article article = articleMapper.findById(articleId);
        if (article == null) {
            return null;
        }
        Map<String, Object> stats = new HashMap<>();
        // 使用独立的浏览记录表统计
        stats.put("views", viewHistoryService.getViewCount(articleId));
        stats.put("uniqueViews", viewHistoryService.getUniqueViewCount(articleId));
        // 使用独立的收藏表统计
        stats.put("favorites", articleFavoriteService.getFavoriteCount(articleId));
        // 点赞和评论数来自已有统计
        stats.put("likeCount", articleLikeMapper.countByArticleId(articleId));
        stats.put("commentCount", commentMapper.listByArticleId(articleId).size());
        return stats;
    }

    //计算文章的热度（使用独立统计表）
    @Override
    public Integer calculateHeat(Integer articleId) {
        Article article = articleMapper.findById(articleId);
        if (article == null) {
            return 0;
        }
        // 使用独立的浏览记录表
        int views = viewHistoryService.getViewCount(articleId);
        int uniqueViews = viewHistoryService.getUniqueViewCount(articleId);
        // 使用独立的收藏表
        int favorites = articleFavoriteService.getFavoriteCount(articleId);
        int likeCount = articleLikeMapper.countByArticleId(articleId);
        int commentCount = commentMapper.listByArticleId(articleId).size();
        // 热度计算公式：浏览量*1 + 独立浏览*3 + 收藏*5 + 点赞*2 + 评论*3
        int heat = views * 1 + uniqueViews * 3 + favorites * 5 + likeCount * 2 + commentCount * 3;
        return heat;
    }
}
