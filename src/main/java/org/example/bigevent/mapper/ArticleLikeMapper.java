package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.*;
import org.example.bigevent.pojo.ArticleLike;

import java.util.List;

@Mapper
public interface ArticleLikeMapper {

    //添加文章点赞
    @Insert("insert into article_like (article_id, user_id, create_time) values (#{articleId}, #{userId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(ArticleLike articleLike);

    //取消文章点赞
    @Delete("delete from article_like where article_id = #{articleId} and user_id = #{userId}")
    void delete(@Param("articleId") Integer articleId, @Param("userId") Integer userId);

    //查询用户的文章点赞记录
    @Select("select * from article_like where user_id = #{userId}")
    List<ArticleLike> listByUserId(@Param("userId") Integer userId);

    //查询用户是否点赞过某文章
    @Select("select * from article_like where article_id = #{articleId} and user_id = #{userId} limit 1")
    ArticleLike findByArticleIdAndUserId(@Param("articleId") Integer articleId, @Param("userId") Integer userId);

    //统计文章点赞数
    @Select("select count(*) from article_like where article_id = #{articleId}")
    Integer countByArticleId(@Param("articleId") Integer articleId);
}
