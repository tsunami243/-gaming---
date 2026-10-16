package org.example.bigevent.service;

import org.example.bigevent.pojo.Category;

import java.util.List;

/** 文章分类业务（legacy 模型，新链路已用「游戏」取代）。 */
public interface CategoryService {

    /**
     * 新增分类，创建人取当前登录用户。
     *
     * @param category 分类实体
     */
    void add(Category category);

    /**
     * 查询当前用户可见的分类列表。
     *
     * @return 分类列表（原始泛型，历史签名）
     */
    List list();

    /**
     * 按 ID 查询分类。
     *
     * @param id 分类 ID
     * @return 分类实体，不存在时返回 {@code null}
     */
    Category findById(Integer id);

    /**
     * 更新分类（需具备归属或管理员权限）。
     *
     * @param category 分类实体
     */
    void update(Category category);

    /**
     * 删除分类。
     *
     * @param id 分类 ID
     */
    void delete(Integer id);
}
