package com.ddhj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ddhj.common.Result;
import com.ddhj.entity.Product;
import com.ddhj.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "商品管理")
@RestController
@RequestMapping("/products")
public class ProductController {

    @Resource
    private ProductService productService;

    @Operation(summary = "获取商品列表")
    @GetMapping
    public Result<Page<Product>> getProductList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Product> page = productService.getProductList(categoryId, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "获取所有商品（不分页）")
    @GetMapping("/all")
    public Result<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return Result.success(products);
    }

    @Operation(summary = "获取商品详情")
    @GetMapping("/{id}")
    public Result<Product> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return Result.success(product);
    }

    @Operation(summary = "添加商品")
    @PostMapping
    public Result<Product> addProduct(@RequestBody Product product) {
        boolean success = productService.addProduct(product);
        return success ? Result.success(product) : Result.error("添加失败");
    }

    @Operation(summary = "更新商品")
    @PutMapping("/{id}")
    public Result<Void> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        boolean success = productService.updateProduct(product);
        return success ? Result.success() : Result.error("更新失败");
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        boolean success = productService.deleteProduct(id);
        return success ? Result.success() : Result.error("删除失败");
    }
}
