package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.*;
import org.example.bigevent.pojo.CommentLike;

import java.util.List;

@Mapper
public interface CommentLikeMapper {

    //添加评论点赞
    @Insert("insert into comment_like (comment_id, user_id, create_time) values (#{commentId}, #{userId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(CommentLike commentLike);

    //取消评论点赞
    @Delete("delete from comment_like where comment_id = #{commentId} and user_id = #{userId}")
    void delete(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    //查询用户的评论点赞记录
    @Select("select * from comment_like where user_id = #{userId}")
    List<CommentLike> listByUserId(@Param("userId") Integer userId);

    //查询用户是否点赞过某评论
    @Select("select * from comment_like where comment_id = #{commentId} and user_id = #{userId} limit 1")
    CommentLike findByCommentIdAndUserId(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    //统计评论点赞数
    @Select("select count(*) from comment_like where comment_id = #{commentId}")
    Integer countByCommentId(@Param("commentId") Integer commentId);
}
