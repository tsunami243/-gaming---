package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.bigevent.pojo.CommentLike;

import java.util.List;

/** 评论点赞记录读写。 */
@Mapper
public interface CommentLikeMapper {

    /**
     * 新增评论点赞记录，并回填自增主键。
     *
     * @param commentLike 点赞实体（需含 commentId、userId 与 createTime）
     */
    @Insert("insert into comment_like (comment_id, user_id, create_time) values (#{commentId}, #{userId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(CommentLike commentLike);

    /**
     * 取消评论点赞。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     */
    @Delete("delete from comment_like where comment_id = #{commentId} and user_id = #{userId}")
    void delete(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    /**
     * 查询某用户的评论点赞记录。
     *
     * @param userId 用户 ID
     * @return 点赞记录列表
     */
    @Select("select * from comment_like where user_id = #{userId}")
    List<CommentLike> listByUserId(@Param("userId") Integer userId);

    /**
     * 查询某用户对某评论的点赞记录。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     * @return 点赞记录，未点赞时返回 {@code null}
     */
    @Select("select * from comment_like where comment_id = #{commentId} and user_id = #{userId} limit 1")
    CommentLike findByCommentIdAndUserId(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    /**
     * 统计评论的点赞总数。
     *
     * @param commentId 评论 ID
     * @return 点赞数
     */
    @Select("select count(*) from comment_like where comment_id = #{commentId}")
    Integer countByCommentId(@Param("commentId") Integer commentId);
}
