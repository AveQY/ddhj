package com.ddhj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ddhj.entity.Specification;
import com.ddhj.mapper.SpecificationMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class SpecificationService {

    @Resource
    private SpecificationMapper specificationMapper;

    public List<Specification> getSpecificationsByProductId(Long productId) {
        LambdaQueryWrapper<Specification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Specification::getProductId, productId);
        return specificationMapper.selectList(wrapper);
    }

    public Specification getSpecificationById(Long id) {
        return specificationMapper.selectById(id);
    }

    public boolean addSpecification(Specification specification) {
        return specificationMapper.insert(specification) > 0;
    }

    public boolean updateSpecification(Specification specification) {
        return specificationMapper.updateById(specification) > 0;
    }

    public boolean deleteSpecification(Long id) {
        return specificationMapper.deleteById(id) > 0;
    }

    public boolean checkStock(Long specId, Integer quantity) {
        Specification spec = specificationMapper.selectById(specId);
        return spec != null && spec.getStock() >= quantity;
    }

    public boolean deductStock(Long specId, Integer quantity) {
        Specification spec = specificationMapper.selectById(specId);
        if (spec == null || spec.getStock() < quantity) {
            return false;
        }
        spec.setStock(spec.getStock() - quantity);
        return specificationMapper.updateById(spec) > 0;
    }
}
