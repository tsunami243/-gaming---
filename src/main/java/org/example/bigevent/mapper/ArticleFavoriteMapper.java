package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.bigevent.pojo.ArticleFavorite;

import java.util.List;

/** 文章收藏记录读写。 */
@Mapper
public interface ArticleFavoriteMapper {

    /**
     * 新增收藏记录，并回填自增主键。
     *
     * @param favorite 收藏实体（需含 articleId、userId 与 createTime）
     */
    @Insert("insert into article_favorite (article_id, user_id, create_time) values (#{articleId}, #{userId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(ArticleFavorite favorite);

    /**
     * 取消收藏。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     */
    @Delete("delete from article_favorite where article_id = #{articleId} and user_id = #{userId}")
    void delete(@Param("articleId") Integer articleId, @Param("userId") Integer userId);

    /**
     * 统计文章的收藏总数。
     *
     * @param articleId 文章 ID
     * @return 收藏数
     */
    @Select("select count(*) from article_favorite where article_id = #{articleId}")
    Integer countByArticleId(@Param("articleId") Integer articleId);

    /**
     * 查询某用户对某文章的收藏记录。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 收藏记录，未收藏时返回 {@code null}
     */
    @Select("select * from article_favorite where article_id = #{articleId} and user_id = #{userId} limit 1")
    ArticleFavorite findByArticleIdAndUserId(@Param("articleId") Integer articleId, @Param("userId") Integer userId);

    /**
     * 查询某用户的收藏记录，按收藏时间倒序。
     *
     * @param userId 用户 ID
     * @return 收藏记录列表
     */
    @Select("select * from article_favorite where user_id = #{userId} order by create_time desc")
    List<ArticleFavorite> listByUserId(@Param("userId") Integer userId);

    /**
     * 统计某用户对某文章的收藏条数（用于判断是否已收藏）。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 条数，0 表示未收藏
     */
    @Select("select count(*) from article_favorite where article_id = #{articleId} and user_id = #{userId}")
    Integer countByArticleIdAndUserId(@Param("articleId") Integer articleId, @Param("userId") Integer userId);
}
