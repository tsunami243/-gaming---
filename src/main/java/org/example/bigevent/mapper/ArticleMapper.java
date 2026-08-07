package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.*;
import org.example.bigevent.pojo.Article;

import java.util.List;

@Mapper
public interface ArticleMapper {
    @Insert("insert into article (title,content,cover_img,state,category_id,create_user,create_time,update_time,tags,is_carousel) values (#{title},#{content},#{coverImg},#{state},#{categoryId},#{createUser},#{createTime},#{updateTime},#{tags},#{isCarousel})")
    void add(Article article);

    List<Article> list(@Param("categoryId") Integer categoryId, @Param("state") String state, @Param("userId") Integer userId);
    //@Param注解用于将方法参数绑定到SQL语句中的占位符
    @Update("update article set title=#{article.title},content=#{article.content},cover_img=#{article.coverImg},state=#{article.state},category_id=#{article.categoryId},update_time=#{article.updateTime} where id=#{article.id} and create_user=#{article.createUser}")
    void update(@Param("article") Article article);
    //根据文章id查询文章详情（不校验用户，公开访问用）
    @Select("select * from article where id=#{id}")
    Article detail(@Param("id") Integer id);

    //根据文章id和用户id查询文章详情（校验用户，编辑/删除用）
    @Select("select * from article where id=#{id} and create_user=#{userId}")
    Article detailByUser(@Param("id") Integer id, @Param("userId") Integer userId);

    //根据文章id查询文章（不校验用户，用于统计）
    @Select("select * from article where id=#{id}")
    Article findById(@Param("id") Integer id);

    //根据文章id删除文章
    @Delete("delete from article where id=#{id} and create_user=#{userId}")
    void delete(@Param("id") Integer id, @Param("userId") Integer userId);

    //查询轮播图文章列表
    @Select("select * from article where is_carousel = 1 and state = '已发布' order by update_time desc limit #{limit}")
    List<Article> selectCarouselList(@Param("limit") Integer limit);


    //根据标签查询文章列表
    List<Article> searchByTag(@Param("tag") String tag);

    //ZSet 热度回写 article.heat
    @Update("update article set heat=#{heat}, heat_updated_at=now() where id=#{id}")
    void updateHeat(@Param("id") Integer id, @Param("heat") Integer heat);

    //清空轮播标记
    @Update("update article set is_carousel=0")
    void resetCarousel();

    //设置轮播标记
    @Update("<script>update article set is_carousel=1 where id in <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    void setCarousel(@Param("ids") List<Integer> ids);

    //按 ID 批量查询文章
    @Select("<script>select * from article where id in <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Article> selectByIds(@Param("ids") List<Integer> ids);

    //MySQL 降级/兜底：按 heat 排序取 TopN
    @Select("select id from article where state='已发布' order by heat desc, id asc limit #{limit}")
    List<Integer> selectHotIdsFromDb(@Param("limit") Integer limit);

    //全量热度（启动重建 ZSet 用）
    @Select("select id, heat from article where state='已发布'")
    List<Article> selectAllHeat();
}
