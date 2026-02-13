package com.ddhj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ddhj.entity.Product;
import com.ddhj.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ProductService {
    
    @Resource
    private ProductMapper productMapper;
    
    public Page<Product> getProductList(Long categoryId, Integer pageNum, Integer pageSize) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null && categoryId > 0) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        return productMapper.selectPage(page, wrapper);
    }
    
    public Product getProductById(Long id) {
        return productMapper.selectById(id);
    }
    
    public boolean addProduct(Product product) {
        return productMapper.insert(product) > 0;
    }
    
    public boolean updateProduct(Product product) {
        return productMapper.updateById(product) > 0;
    }
    
    public boolean deleteProduct(Long id) {
        return productMapper.deleteById(id) > 0;
    }
    
    public List<Product> getAllProducts() {
        return productMapper.selectList(null);
    }
}
