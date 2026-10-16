package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.bigevent.pojo.Category;

import java.util.List;

/**
 * 文章分类数据访问（legacy 模型）。
 *
 * <p>注意 {@link #finById(Integer)} 是既有拼写（应为 findById），
 * 改名会波及调用方，故保持原样。</p>
 */
@Mapper
public interface CategoryMapper {

    /**
     * 新增分类。
     *
     * @param category 分类实体（需含名称、别名与创建人）
     */
    @Insert("insert into category(category_name,category_alias,create_user,create_time,update_time) " +
            "values(#{categoryName},#{categoryAlias},#{createUser},#{createTime},#{updateTime})")
    void add(Category category);

    /**
     * 查询分类列表，可按创建人过滤。
     *
     * @param userId 创建人过滤，为 null 表示查询全部
     * @return 分类列表
     */
    @Select("<script>select * from category <where><if test='userId != null'>create_user = #{userId}</if></where></script>")
    List<Category> list(@Param("userId") Integer userId);

    /**
     * 按 ID 查询分类。
     *
     * @param id 分类 ID
     * @return 分类实体，不存在时返回 {@code null}
     */
    @Select("select * from category where id = #{id}")
    Category finById(Integer id);

    /**
     * 更新分类名称与别名。
     *
     * @param category 分类实体（需含 id）
     */
    @Update("update category set category_name = #{categoryName},category_alias = #{categoryAlias},update_time = #{updateTime} where id = #{id} ")
    void update(Category category);

    /**
     * 按 ID 删除分类。
     *
     * @param id 分类 ID
     */
    @Delete("delete from category where id = #{id}")
    void delete(Integer id);

    /**
     * 查询某创建人名下的全部分类 ID（删除用户前清理关联用）。
     *
     * @param userId 创建人 ID
     * @return 分类 ID 列表
     */
    @Select("select id from category where create_user = #{userId}")
    List<Integer> findIds(Integer userId);
}
