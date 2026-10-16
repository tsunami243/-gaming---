package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.bigevent.pojo.ArticleViewHistory;

/** 文章浏览历史读写（记录颗粒度：一次成功入库的浏览 = 一行）。 */
@Mapper
public interface ArticleViewHistoryMapper {

    /**
     * 新增浏览记录，并回填自增主键。
     *
     * <p>去重由服务层在插入前查询 24 小时内的既有记录决定，本方法不做去重。</p>
     *
     * @param viewHistory 浏览记录实体
     */
    @Insert("insert into article_view_history (article_id, user_id, ip_address, view_time) values (#{articleId}, #{userId}, #{ipAddress}, #{viewTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(ArticleViewHistory viewHistory);

    /**
     * 统计文章总浏览量。
     *
     * @param articleId 文章 ID
     * @return 浏览总次数
     */
    @Select("select count(*) from article_view_history where article_id = #{articleId}")
    Integer countByArticleId(@Param("articleId") Integer articleId);

    /**
     * 统计独立浏览量（按 user_id 去重，忽略游客）。
     *
     * @param articleId 文章 ID
     * @return 去重后的用户数
     */
    @Select("select count(distinct user_id) from article_view_history where article_id = #{articleId} and user_id is not null")
    Integer countUniqueViewsByUserId(@Param("articleId") Integer articleId);

    /**
     * 查询该用户在 24 小时内是否已浏览过（去重判定用）。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 窗口内的最近一条记录，无则返回 {@code null}
     */
    @Select("select * from article_view_history where article_id = #{articleId} and user_id = #{userId} and view_time > date_sub(now(), interval 24 hour) limit 1")
    ArticleViewHistory findRecentByUserId(@Param("articleId") Integer articleId, @Param("userId") Integer userId);

    /**
     * 查询该 IP 在 24 小时内是否已浏览过（游客去重判定用）。
     *
     * @param articleId 文章 ID
     * @param ipAddress 访客 IP
     * @return 窗口内的最近一条记录，无则返回 {@code null}
     */
    @Select("select * from article_view_history where article_id = #{articleId} and ip_address = #{ipAddress} and user_id is null and view_time > date_sub(now(), interval 24 hour) limit 1")
    ArticleViewHistory findRecentByIpAddress(@Param("articleId") Integer articleId, @Param("ipAddress") String ipAddress);
}
