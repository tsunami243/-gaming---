package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.bigevent.pojo.ArticleLike;

import java.util.List;

/** 文章点赞记录读写。 */
@Mapper
public interface ArticleLikeMapper {

    /**
     * 新增点赞记录，并回填自增主键。
     *
     * @param articleLike 点赞实体（需含 articleId、userId 与 createTime）
     */
    @Insert("insert into article_like (article_id, user_id, create_time) values (#{articleId}, #{userId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(ArticleLike articleLike);

    /**
     * 取消点赞。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     */
    @Delete("delete from article_like where article_id = #{articleId} and user_id = #{userId}")
    void delete(@Param("articleId") Integer articleId, @Param("userId") Integer userId);

    /**
     * 查询某用户的点赞记录。
     *
     * @param userId 用户 ID
     * @return 点赞记录列表
     */
    @Select("select * from article_like where user_id = #{userId}")
    List<ArticleLike> listByUserId(@Param("userId") Integer userId);

    /**
     * 查询某用户对某文章的点赞记录。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 点赞记录，未点赞时返回 {@code null}
     */
    @Select("select * from article_like where article_id = #{articleId} and user_id = #{userId} limit 1")
    ArticleLike findByArticleIdAndUserId(@Param("articleId") Integer articleId, @Param("userId") Integer userId);

    /**
     * 统计文章的点赞总数。
     *
     * @param articleId 文章 ID
     * @return 点赞数
     */
    @Select("select count(*) from article_like where article_id = #{articleId}")
    Integer countByArticleId(@Param("articleId") Integer articleId);
}
