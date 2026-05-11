package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.*;
import org.example.bigevent.pojo.ArticleFavorite;

import java.util.List;

@Mapper
public interface ArticleFavoriteMapper {

    // 添加收藏
    @Insert("insert into article_favorite (article_id, user_id, create_time) values (#{articleId}, #{userId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(ArticleFavorite favorite);

    // 取消收藏
    @Delete("delete from article_favorite where article_id = #{articleId} and user_id = #{userId}")
    void delete(@Param("articleId") Integer articleId, @Param("userId") Integer userId);

    // 统计收藏数
    @Select("select count(*) from article_favorite where article_id = #{articleId}")
    Integer countByArticleId(@Param("articleId") Integer articleId);

    // 检查是否已收藏
    @Select("select * from article_favorite where article_id = #{articleId} and user_id = #{userId} limit 1")
    ArticleFavorite findByArticleIdAndUserId(@Param("articleId") Integer articleId, @Param("userId") Integer userId);

    // 获取用户收藏列表
    @Select("select * from article_favorite where user_id = #{userId} order by create_time desc")
    List<ArticleFavorite> listByUserId(@Param("userId") Integer userId);

    // 获取用户对某文章的收藏（用于判断是否收藏）
    @Select("select count(*) from article_favorite where article_id = #{articleId} and user_id = #{userId}")
    Integer countByArticleIdAndUserId(@Param("articleId") Integer articleId, @Param("userId") Integer userId);
}
