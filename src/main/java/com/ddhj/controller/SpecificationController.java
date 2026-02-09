package com.ddhj.controller;

import com.ddhj.common.Result;
import com.ddhj.entity.Specification;
import com.ddhj.service.SpecificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "规格管理")
@RestController
@RequestMapping("/specifications")
public class SpecificationController {
    
    @Resource
    private SpecificationService specificationService;
    
    @Operation(summary = "根据商品ID获取规格列表")
    @GetMapping("/product/{productId}")
    public Result<List<Specification>> getSpecificationsByProductId(@PathVariable Long productId) {
        List<Specification> specifications = specificationService.getSpecificationsByProductId(productId);
        return Result.success(specifications);
    }
    
    @Operation(summary = "获取规格详情")
    @GetMapping("/{id}")
    public Result<Specification> getSpecificationById(@PathVariable Long id) {
        Specification specification = specificationService.getSpecificationById(id);
        return Result.success(specification);
    }
    
    @Operation(summary = "添加规格")
    @PostMapping
    public Result<Void> addSpecification(@RequestBody Specification specification) {
        boolean success = specificationService.addSpecification(specification);
        return success ? Result.success() : Result.error("添加失败");
    }
    
    @Operation(summary = "更新规格")
    @PutMapping("/{id}")
    public Result<Void> updateSpecification(@PathVariable Long id, @RequestBody Specification specification) {
        specification.setId(id);
        boolean success = specificationService.updateSpecification(specification);
        return success ? Result.success() : Result.error("更新失败");
    }
    
    @Operation(summary = "删除规格")
    @DeleteMapping("/{id}")
    public Result<Void> deleteSpecification(@PathVariable Long id) {
        boolean success = specificationService.deleteSpecification(id);
        return success ? Result.success() : Result.error("删除失败");
    }
}
