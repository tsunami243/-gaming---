package org.example.bigevent.controller;

import org.example.bigevent.pojo.Category;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类管理（legacy 接口）。
 *
 * <p>基础路径 {@code /category}；读操作公开，写操作需 CONTENT_ADMIN / SUPER_ADMIN 角色。
 * 分类是旧数据模型的一部分，新前端游戏筛选已改用 {@code /api/v1/games}。
 */
@RestController
@Validated
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    CategoryService categoryService;

    /**
     * 新增分类（需管理员）。
     *
     * <p>{@code POST /category}
     *
     * @param category 分类数据（Add 校验组）
     * @return 空数据
     */
    @PostMapping
    public Result add(@RequestBody @Validated(Category.Add.class) Category category) {
        categoryService.add(category);
        return Result.success();
    }

    /**
     * 分类列表（公开）。
     *
     * <p>{@code GET /category}
     *
     * @return 全部分类
     */
    @GetMapping
    public Result<List<Category>> list() {
        List<Category> cs = categoryService.list();
        return Result.success(cs);
    }

    /**
     * 分类详情（公开）。
     *
     * <p>{@code GET /category/detail?id=}
     *
     * @param id 分类 ID
     * @return 分类详情
     */
    @GetMapping("/detail")
    public Result<Category> detail(Integer id) {
        Category c = categoryService.findById(id);
        return Result.success(c);
    }

    /**
     * 更新分类（需管理员）。
     *
     * <p>{@code PUT /category}
     *
     * @param category 分类数据（Update 校验组）
     * @return 空数据
     */
    @PutMapping
    public Result update(@RequestBody @Validated(Category.Update.class) Category category) {
        categoryService.update(category);
        return Result.success();
    }

    /**
     * 删除分类（需管理员）。
     *
     * <p>{@code DELETE /category?id=}
     *
     * @param id 分类 ID；为空时返回错误
     * @return 空数据
     */
    @DeleteMapping
    public Result delete(Integer id) {
        if (id == null) {
            return Result.error("id不能为空");
        }
        categoryService.delete(id);
        return Result.success();
    }
}
