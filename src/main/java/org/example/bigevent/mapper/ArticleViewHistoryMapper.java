package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.*;
import org.example.bigevent.pojo.ArticleViewHistory;

@Mapper
public interface ArticleViewHistoryMapper {

    // 记录浏览（防重复，24小时内同一用户/IP只算一次）
    @Insert("insert into article_view_history (article_id, user_id, ip_address, view_time) values (#{articleId}, #{userId}, #{ipAddress}, #{viewTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(ArticleViewHistory viewHistory);

    // 统计文章总浏览量
    @Select("select count(*) from article_view_history where article_id = #{articleId}")
    Integer countByArticleId(@Param("articleId") Integer articleId);

    // 统计独立浏览量（去重，按user_id）
    @Select("select count(distinct user_id) from article_view_history where article_id = #{articleId} and user_id is not null")
    Integer countUniqueViewsByUserId(@Param("articleId") Integer articleId);

    // 检查用户是否在24小时内浏览过（防重复记录）
    @Select("select * from article_view_history where article_id = #{articleId} and user_id = #{userId} and view_time > date_sub(now(), interval 24 hour) limit 1")
    ArticleViewHistory findRecentByUserId(@Param("articleId") Integer articleId, @Param("userId") Integer userId);

    // 检查IP是否在24小时内浏览过（游客去重）
    @Select("select * from article_view_history where article_id = #{articleId} and ip_address = #{ipAddress} and user_id is null and view_time > date_sub(now(), interval 24 hour) limit 1")
    ArticleViewHistory findRecentByIpAddress(@Param("articleId") Integer articleId, @Param("ipAddress") String ipAddress);
}
