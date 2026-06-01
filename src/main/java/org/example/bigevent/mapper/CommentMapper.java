package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.*;
import org.example.bigevent.pojo.Comment;

import java.util.List;

@Mapper
public interface CommentMapper {

    //添加评论
    @Insert("insert into comment (article_id, user_id, parent_id, content, create_time) values (#{articleId}, #{userId}, #{parentId}, #{content}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(Comment comment);

    //根据文章ID查询评论列表
    @Select("select * from comment where article_id = #{articleId} order by create_time desc")
    List<Comment> listByArticleId(@Param("articleId") Integer articleId);

    //根据文章ID查询顶级评论（parent_id为null）
    @Select("select * from comment where article_id = #{articleId} and parent_id is null order by create_time desc")
    List<Comment> listTopLevelByArticleId(@Param("articleId") Integer articleId);

    //根据父评论ID查询回复列表
    @Select("select * from comment where parent_id = #{parentId} order by create_time asc")
    List<Comment> listRepliesByParentId(@Param("parentId") Integer parentId);

    //根据评论ID查询评论详情
    @Select("select * from comment where id = #{id}")
    Comment detail(@Param("id") Integer id);

    //删除评论
    @Delete("delete from comment where id = #{id} and user_id = #{userId}")
    void delete(@Param("id") Integer id, @Param("userId") Integer userId);

    //更新评论点赞数
    @Update("update comment set like_count = #{likeCount} where id = #{id}")
    void updateLikeCount(@Param("id") Integer id, @Param("likeCount") Integer likeCount);
}
