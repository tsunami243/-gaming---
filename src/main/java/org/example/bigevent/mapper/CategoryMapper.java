package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.*;
import org.example.bigevent.pojo.Category;

import java.util.List;

@Mapper
public interface CategoryMapper {
    @Insert("insert into category(category_name,category_alias,create_user,create_time,update_time) " +
            "values(#{categoryName},#{categoryAlias},#{createUser},#{createTime},#{updateTime})")
    void add(Category category);

    @Select("<script>select * from category <where><if test='userId != null'>create_user = #{userId}</if></where></script>")
    List<Category> list(@Param("userId") Integer userId);

    @Select("select * from category where id = #{id}")
    Category finById(Integer id);

    @Update("update category set category_name = #{categoryName},category_alias = #{categoryAlias},update_time = #{updateTime} where id = #{id} ")
    void update(Category category);

    @Delete("delete from category where id = #{id}")
    void delete(Integer id);

    @Select("select id from category where create_user = #{userId}")
    List<Integer> findIds(Integer userId);
}
