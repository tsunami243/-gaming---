package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.CategoryMapper;
import org.example.bigevent.pojo.Category;
import org.example.bigevent.service.CategoryService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 分类（legacy 数据模型）的业务实现。
 *
 * <p>分类是旧数据模型的一部分，新前端的游戏筛选已改用 {@code /api/v1/games}。
 * 本实现仍保留创建人归属校验：只有创建者本人可以删除自己创建的分类。
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 新增分类，创建人取自当前登录态。
     *
     * @param category 分类数据
     */
    @Override
    public void add(Category category) {
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        category.setCreateUser(userId);
        categoryMapper.add(category);
    }

    /**
     * 查询全部分类。
     *
     * @return 所有分类；公开读取，不按用户过滤
     */
    @Override
    public List list() {
        // 公开分类列表不过滤用户，所有用户看到全部分类
        return categoryMapper.list(null);
    }

    /**
     * 按 ID 查询分类。
     *
     * @param id 分类 ID
     * @return 分类；不存在返回 {@code null}
     */
    @Override
    public Category findById(Integer id) {
        return categoryMapper.finById(id);
    }

    /**
     * 更新分类。
     *
     * @param category 分类数据，须含 ID
     */
    @Override
    public void update(Category category) {
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.update(category);
    }

    /**
     * 删除分类；仅创建者本人可删除。
     *
     * @param id 分类 ID
     * @throws RuntimeException 分类不存在，或当前用户不是创建者
     */
    @Override
    public void delete(Integer id) {
        Category category = categoryMapper.finById(id);
        if (category == null) {
            throw new RuntimeException("该分类不存在");
        }
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        if (!userId.equals(category.getCreateUser())) {
            throw new RuntimeException("无权删除他人分类");
        }
        categoryMapper.delete(id);
    }
}
