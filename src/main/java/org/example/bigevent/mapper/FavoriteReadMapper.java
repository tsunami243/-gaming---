package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.bigevent.pojo.dto.FavoriteSummary;

import java.util.List;

/**
 * 收藏读取：分页列出「我收藏的已发布文章」。
 */
@Mapper
public interface FavoriteReadMapper {

    /** 列表与计数的公共 FROM/WHERE 片段：只统计已发布文章，下架稿件自动从收藏列表消失。 */
    String FROM = " from article_favorite f join article a on a.id=f.article_id where f.user_id=#{userId} and a.state='已发布' and a.workflow_status='PUBLISHED'";

    /**
     * 分页查询收藏项，按收藏时间倒序。
     *
     * @param userId 用户 ID
     * @param limit  每页条数
     * @param offset 偏移量
     * @return 收藏摘要列表
     */
    @Select("select a.id,a.title,a.summary,f.create_time as savedAt" + FROM + " order by f.create_time desc,f.id desc limit #{limit} offset #{offset}")
    List<FavoriteSummary> page(@Param("userId") int userId, @Param("limit") int limit, @Param("offset") long offset);

    /**
     * 统计收藏总数。
     *
     * @param userId 用户 ID
     * @return 收藏总数
     */
    @Select("select count(*)" + FROM)
    long count(@Param("userId") int userId);
}
