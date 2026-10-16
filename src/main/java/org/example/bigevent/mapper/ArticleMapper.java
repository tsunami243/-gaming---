package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.bigevent.pojo.Article;

import java.util.List;

/**
 * 文章主表数据访问。
 *
 * <p>公开读取相关查询（{@code listPublishedByFilters} / {@code detailPublished} /
 * {@code selectPublishedContentById}）一律带上「已发布」双重条件
 * （{@code state='已发布' and workflow_status='PUBLISHED'}），草稿与驳回稿件不会外泄。</p>
 *
 * <p>部分 SQL 定义在同名 XML（{@code ArticleMapper.xml}）中，改动时需两处对照。</p>
 */
@Mapper
public interface ArticleMapper {

    /**
     * 新增文章。
     *
     * @param article 文章实体
     */
    @Insert("insert into article (title,content,cover_img,state,category_id,create_user,create_time,update_time,tags,is_carousel) values (#{title},#{content},#{coverImg},#{state},#{categoryId},#{createUser},#{createTime},#{updateTime},#{tags},#{isCarousel})")
    void add(Article article);

    /**
     * 按分类/状态/作者查询文章（SQL 在 XML 中）。
     *
     * @param categoryId 分类过滤，可为 null
     * @param state      发布状态过滤，可为 null
     * @param userId     作者过滤，为 null 表示不过滤
     * @return 文章列表
     */
    List<Article> list(@Param("categoryId") Integer categoryId, @Param("state") String state, @Param("userId") Integer userId);

    /** Public v1 reads use the normalized game relation and stable content type. */
    /**
     * 公开列表查询：按游戏与内容类型筛选已发布文章（SQL 在 XML 中）。
     *
     * @param gameCode    游戏编码，可为 null
     * @param contentType 内容类型，可为 null
     * @return 已发布文章列表（不含正文列）
     */
    List<Article> listPublishedByFilters(@Param("gameCode") String gameCode, @Param("contentType") String contentType);

    /**
     * 作者更新自己的文章（WHERE 带 create_user 限定，越权更新不会生效）。
     *
     * @param article 文章实体，需含 id 与 createUser
     */
    @Update("update article set title=#{article.title},content=#{article.content},cover_img=#{article.coverImg},state=#{article.state},category_id=#{article.categoryId},update_time=#{article.updateTime} where id=#{article.id} and create_user=#{article.createUser}")
    void update(@Param("article") Article article);

    /**
     * 管理员更新文章（WHERE 不带作者限定，可跨用户修改）。
     *
     * @param article 文章实体，需含 id
     */
    @Update("update article set title=#{article.title},content=#{article.content},cover_img=#{article.coverImg},state=#{article.state},category_id=#{article.categoryId},update_time=#{article.updateTime} where id=#{article.id}")
    void updateByAdmin(@Param("article") Article article);

    /**
     * 按 ID 查询文章详情（不校验状态与用户，公开访问用）。
     *
     * @param id 文章 ID
     * @return 文章实体，不存在时返回 {@code null}
     */
    @Select("select * from article where id=#{id}")
    Article detail(@Param("id") Integer id);

    // Public visibility query: drafts/rejected articles must not leak through read APIs.
    /**
     * 按 ID 查询<b>已发布</b>文章（草稿与驳回稿件不可见）。
     *
     * @param id 文章 ID
     * @return 文章实体，未发布时返回 {@code null}
     */
    @Select("select * from article where id=#{id} and state='已发布' and workflow_status='PUBLISHED'")
    Article detailPublished(@Param("id") Integer id);

    /**
     * 公开详情读取：显式列出标题、正文与展示所需的元数据字段。
     *
     * <p>只返回已发布文章；正文来自 article.content（入库时写入的爬取正文）。</p>
     *
     * @param id 文章 ID
     * @return 含正文的文章实体，未发布时返回 {@code null}
     */
    @Select("select id, title, summary, content, cover_img, content_type, reading_mode, workflow_status, "
            + "source_language, original_url, tags, original_published_at, original_published_date, "
            + "ingested_at, published_at from article "
            + "where id=#{id} and state='已发布' and workflow_status='PUBLISHED'")
    Article selectPublishedContentById(@Param("id") Integer id);

    /**
     * 按文章 ID + 作者查询详情（编辑/删除前的归属校验用）。
     *
     * @param id     文章 ID
     * @param userId 作者用户 ID
     * @return 文章实体，非本人文章返回 {@code null}
     */
    @Select("select * from article where id=#{id} and create_user=#{userId}")
    Article detailByUser(@Param("id") Integer id, @Param("userId") Integer userId);

    /**
     * 按 ID 查询文章（不校验状态与用户，互动统计用）。
     *
     * @param id 文章 ID
     * @return 文章实体，不存在时返回 {@code null}
     */
    @Select("select * from article where id=#{id}")
    Article findById(@Param("id") Integer id);

    /**
     * 删除自己的文章（WHERE 带 create_user 限定）。
     *
     * @param id     文章 ID
     * @param userId 作者用户 ID
     */
    @Delete("delete from article where id=#{id} and create_user=#{userId}")
    void delete(@Param("id") Integer id, @Param("userId") Integer userId);

    /**
     * 按 ID 删除文章（管理员用，不限作者）。
     *
     * @param id 文章 ID
     */
    @Delete("delete from article where id=#{id}")
    void deleteById(@Param("id") Integer id);

    /**
     * 查询轮播位文章。
     *
     * @param limit 条数上限
     * @return 轮播文章列表（按更新时间倒序）
     */
    @Select("select * from article where is_carousel = 1 and state = '已发布' and workflow_status='PUBLISHED' order by update_time desc limit #{limit}")
    List<Article> selectCarouselList(@Param("limit") Integer limit);

    /**
     * 按标签查询文章（SQL 在 XML 中）。
     *
     * @param tag   标签关键字
     * @param state 发布状态过滤
     * @return 文章列表
     */
    List<Article> searchByTag(@Param("tag") String tag, @Param("state") String state);

    /**
     * 把热度分回写到 article.heat（ZSet 与库表的同步口径）。
     *
     * @param id   文章 ID
     * @param heat 热度分
     */
    @Update("update article set heat=#{heat}, heat_updated_at=now() where id=#{id}")
    void updateHeat(@Param("id") Integer id, @Param("heat") Integer heat);

    /** 清空全部轮播标记（重建轮播列表前调用）。 */
    @Update("update article set is_carousel=0")
    void resetCarousel();

    /**
     * 批量设置轮播标记。
     *
     * @param ids 需要置为轮播的文章 ID 列表
     */
    @Update("<script>update article set is_carousel=1 where id in <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    void setCarousel(@Param("ids") List<Integer> ids);

    /**
     * 按 ID 批量查询文章（不限状态）。
     *
     * @param ids 文章 ID 列表
     * @return 文章列表
     */
    @Select("<script>select * from article where id in <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Article> selectByIds(@Param("ids") List<Integer> ids);

    /**
     * 按 ID 批量查询<b>已发布</b>文章（热度榜回填用）。
     *
     * @param ids 文章 ID 列表
     * @return 已发布文章列表，未发布者会被过滤掉
     */
    @Select("<script>select * from article where state='已发布' and workflow_status='PUBLISHED' and id in <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Article> selectPublishedByIds(@Param("ids") List<Integer> ids);

    /**
     * MySQL 降级/兜底：按 heat 排序取 TopN 文章 ID（Redis 热度榜不可用时使用）。
     *
     * @param limit 条数上限
     * @return 文章 ID 列表，按热度降序
     */
    @Select("select id from article where state='已发布' and workflow_status='PUBLISHED' order by heat desc, id asc limit #{limit}")
    List<Integer> selectHotIdsFromDb(@Param("limit") Integer limit);

    /**
     * 查询全部已发布文章的热度值（服务启动时重建 ZSet 用）。
     *
     * @return 仅含 id 与 heat 的文章列表
     */
    @Select("select id, heat from article where state='已发布' and workflow_status='PUBLISHED'")
    List<Article> selectAllHeat();
}
