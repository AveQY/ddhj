package com.ddhj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ddhj.entity.Order;
import com.ddhj.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private SpecificationService specificationService;

    public String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d", new Random().nextInt(1000000));
        return timestamp + random;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean createOrder(Order order) {
        if (order.getOrderNumber() == null || order.getOrderNumber().isEmpty()) {
            order.setOrderNumber(generateOrderNumber());
        }
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }

        boolean success = orderMapper.insert(order) > 0;

        if (success && order.getItems() != null) {
            for (Map.Entry<String, Object> entry : order.getItems().entrySet()) {
                String key = entry.getKey();
                if ("notes".equals(key))
                    continue;

                Object value = entry.getValue();
                if (value instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> itemsList = (List<Map<String, Object>>) value;
                    for (Map<String, Object> item : itemsList) {
                        Object specIdObj = item.get("规格id");
                        Object quantityObj = item.get("购买数量");

                        if (specIdObj != null) {
                            Long specId = Long.parseLong(specIdObj.toString());
                            int quantity = Integer.parseInt(quantityObj.toString());
                            if (!specificationService.deductStock(specId, quantity)) {
                                throw new RuntimeException("商品库存不足，下单失败");
                            }
                        }
                    }
                }
            }
        }

        return success;
    }

    public Page<Order> getOrderList(Integer pageNum, Integer pageSize, LocalDateTime startDate, LocalDateTime endDate) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null) {
            wrapper.ge(Order::getOrderDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(Order::getOrderDate, endDate);
        }
        wrapper.orderByDesc(Order::getOrderDate);
        return orderMapper.selectPage(page, wrapper);
    }

    public Order getOrderById(Long id) {
        return orderMapper.selectById(id);
    }

    public Order getOrderByNumber(String orderNumber) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNumber, orderNumber);
        return orderMapper.selectOne(wrapper);
    }

    public boolean deleteOrder(Long id) {
        return orderMapper.deleteById(id) > 0;
    }
}
