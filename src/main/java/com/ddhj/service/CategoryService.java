package com.ddhj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ddhj.entity.Category;
import com.ddhj.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    public List<Category> getAllCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getCreateTime);
        return categoryMapper.selectList(wrapper);
    }

    public Category getCategoryById(Long id) {
        return categoryMapper.selectById(id);
    }

    public boolean addCategory(Category category) {
        return categoryMapper.insert(category) > 0;
    }

    public boolean updateCategory(Category category) {
        return categoryMapper.updateById(category) > 0;
    }

    public boolean deleteCategory(Long id) {
        return categoryMapper.deleteById(id) > 0;
    }

    public boolean updateSortOrder(List<Long> ids) {
        for (int i = 0; i < ids.size(); i++) {
            Category category = new Category();
            category.setId(ids.get(i));
            category.setSortOrder(i);
            categoryMapper.updateById(category);
        }
        return true;
    }
}
