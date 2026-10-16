package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.bigevent.pojo.Comment;

import java.util.List;

/**
 * 评论记录读写（legacy 模型）。
 *
 * <p>公开评论列表走 {@code PublicCommentMapper}（会校验文章已发布）；
 * 本接口不做发布状态校验，调用方需自行保证可见性。</p>
 */
@Mapper
public interface CommentMapper {

    /**
     * 新增评论，并回填自增主键。
     *
     * @param comment 评论实体（需含 articleId、userId、parentId 与 content）
     */
    @Insert("insert into comment (article_id, user_id, parent_id, content, create_time) values (#{articleId}, #{userId}, #{parentId}, #{content}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(Comment comment);

    /**
     * 查询某篇文章的全部评论，按时间倒序。
     *
     * @param articleId 文章 ID
     * @return 评论列表
     */
    @Select("select * from comment where article_id = #{articleId} order by create_time desc")
    List<Comment> listByArticleId(@Param("articleId") Integer articleId);

    /**
     * 查询某篇文章的顶级评论（{@code parent_id} 为 null），按时间倒序。
     *
     * @param articleId 文章 ID
     * @return 顶级评论列表
     */
    @Select("select * from comment where article_id = #{articleId} and parent_id is null order by create_time desc")
    List<Comment> listTopLevelByArticleId(@Param("articleId") Integer articleId);

    /**
     * 查询某条评论的回复列表，按时间正序（对话顺序）。
     *
     * @param parentId 父评论 ID
     * @return 回复列表
     */
    @Select("select * from comment where parent_id = #{parentId} order by create_time asc")
    List<Comment> listRepliesByParentId(@Param("parentId") Integer parentId);

    /**
     * 按 ID 查询评论详情。
     *
     * @param id 评论 ID
     * @return 评论实体，不存在时返回 {@code null}
     */
    @Select("select * from comment where id = #{id}")
    Comment detail(@Param("id") Integer id);

    /**
     * 删除评论（WHERE 带 user_id 限定，仅作者本人可删）。
     *
     * @param id     评论 ID
     * @param userId 当前登录用户 ID
     */
    @Delete("delete from comment where id = #{id} and user_id = #{userId}")
    void delete(@Param("id") Integer id, @Param("userId") Integer userId);

    /**
     * 更新评论点赞数。
     *
     * @param id        评论 ID
     * @param likeCount 目标点赞数
     */
    @Update("update comment set like_count = #{likeCount} where id = #{id}")
    void updateLikeCount(@Param("id") Integer id, @Param("likeCount") Integer likeCount);
}
