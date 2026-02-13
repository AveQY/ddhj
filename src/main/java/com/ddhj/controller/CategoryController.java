package com.ddhj.controller;

import com.ddhj.common.Result;
import com.ddhj.entity.Category;
import com.ddhj.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "分类管理")
@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @Operation(summary = "获取所有分类")
    @GetMapping
    public Result<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return Result.success(categories);
    }

    @Operation(summary = "获取分类详情")
    @GetMapping("/{id}")
    public Result<Category> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        return Result.success(category);
    }

    @Operation(summary = "添加分类")
    @PostMapping
    public Result<Void> addCategory(@RequestBody Category category) {
        boolean success = categoryService.addCategory(category);
        return success ? Result.success() : Result.error("添加失败");
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        boolean success = categoryService.updateCategory(category);
        return success ? Result.success() : Result.error("更新失败");
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        boolean success = categoryService.deleteCategory(id);
        return success ? Result.success() : Result.error("删除失败");
    }

    @Operation(summary = "更新分类排序")
    @PostMapping("/sort")
    public Result<Void> sortCategories(@RequestBody List<Long> ids) {
        boolean success = categoryService.updateSortOrder(ids);
        return success ? Result.success() : Result.error("排序更新失败");
    }
}
