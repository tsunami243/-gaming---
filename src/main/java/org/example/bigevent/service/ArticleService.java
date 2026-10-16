package org.example.bigevent.service;

import org.example.bigevent.pojo.Article;
import org.example.bigevent.pojo.PageBean;

import java.util.List;
import java.util.Map;

/** 文章业务（legacy 模型）：增删改查、轮播、搜索、热度榜与互动统计。 */
public interface ArticleService {

    /**
     * 新增文章，作者取当前登录用户。
     *
     * @param article 文章实体
     */
    void addArticle(Article article);

    /**
     * 新增文章并显式指定作者。
     *
     * @param article    文章实体
     * @param createUser 作者用户 ID
     */
    void addArticle(Article article, Integer createUser);

    /**
     * 分页查询已发布文章。
     *
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @param categoryId 分类过滤，可为 null
     * @param state      该参数被有意忽略，列表强制只看已发布
     * @return 分页结果
     */
    PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state);

    /**
     * 更新文章（含归属校验；管理员走独立更新分支）。
     *
     * @param article 待更新文章
     */
    void update(Article article);

    /**
     * 查询文章详情——只有已发布（公开可见）的文章才会返回。
     *
     * @param id 文章 ID
     * @return 文章实体，不可见时返回 {@code null}
     */
    Article detail(Integer id);

    /**
     * 断言文章已发布可见，否则返回 404。
     *
     * @param id 文章 ID
     */
    void requirePublished(Integer id);

    /**
     * 删除文章（普通用户只能删自己创建的，管理员不限）。
     *
     * @param id 文章 ID
     */
    void delete(Integer id);

    /**
     * 查询轮播位文章。
     *
     * @param limit 条数上限
     * @return 轮播文章列表
     */
    List<Article> getCarouselList(Integer limit);

    /**
     * 统计文章的浏览、收藏、点赞、评论等互动指标。
     *
     * @param articleId 文章 ID
     * @return 指标 Map；文章不存在时返回 {@code null}
     */
    Map<String, Object> getArticleStats(Integer articleId);

    /**
     * 计算文章热度分（ZSet 优先，缺失时按各指标加权求和）。
     *
     * @param articleId 文章 ID
     * @return 热度分
     */
    Integer calculateHeat(Integer articleId);

    /**
     * 按标签分页搜索已发布文章。
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param tag      标签关键字
     * @return 分页结果
     */
    PageBean<Article> search(Integer pageNum, Integer pageSize, String tag);

    /**
     * 查询热度榜 TopN。
     *
     * @param limit 条数上限
     * @return 按热度降序的文章列表
     */
    List<Article> getHotList(Integer limit);
}
