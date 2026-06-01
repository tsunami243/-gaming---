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

@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;
    @Override
    public void add(Category category) {
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        category.setCreateUser(userId);
        categoryMapper.add(category);
    }

    @Override
    public List list() {
        // 公开分类列表不过滤用户，所有用户看到全部分类
        return categoryMapper.list(null);
    }

    @Override
    public Category findById(Integer id) {
        return categoryMapper.finById(id);
    }

    @Override
    public void update(Category category) {
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.update(category);
    }

    @Override
    public void delete(Integer id) {
        Category category = categoryMapper.finById(id);
        if (category == null) {
            throw new RuntimeException("该分类不存在");
        }
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("userId");
        if (!userId.equals(category.getCreateUser())) {
            throw new RuntimeException("无权删除他人分类");
        }
        categoryMapper.delete(id);
    }


}
