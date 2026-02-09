package com.ddhj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ddhj.common.Result;
import com.ddhj.entity.Order;
import com.ddhj.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Resource
    private OrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<String> createOrder(@RequestBody Order order) {
        boolean success = orderService.createOrder(order);
        if (success) {
            return Result.success(order.getOrderNumber());
        }
        return Result.error("创建订单失败");
    }

    @Operation(summary = "获取订单列表")
    @GetMapping
    public Result<Page<Order>> getOrderList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Page<Order> page = orderService.getOrderList(pageNum, pageSize, startDate, endDate);
        return Result.success(page);
    }

    @Operation(summary = "根据ID获取订单详情")
    @GetMapping("/{id}")
    public Result<Order> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return Result.success(order);
    }

    @Operation(summary = "根据订单号获取订单详情")
    @GetMapping("/number/{orderNumber}")
    public Result<Order> getOrderByNumber(@PathVariable String orderNumber) {
        Order order = orderService.getOrderByNumber(orderNumber);
        return Result.success(order);
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        boolean success = orderService.deleteOrder(id);
        return success ? Result.success() : Result.error("删除失败");
    }
}
