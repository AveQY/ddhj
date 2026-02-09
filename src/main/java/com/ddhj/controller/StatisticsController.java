package com.ddhj.controller;

import com.ddhj.common.Result;
import com.ddhj.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "统计数据")
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    @Operation(summary = "获取收入统计")
    @GetMapping("/revenue")
    public Result<Map<String, Object>> getRevenueStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(defaultValue = "hour") String mode) {
        Map<String, Object> data = statisticsService.getRevenueStatistics(date, mode);
        return Result.success(data);
    }

    @Operation(summary = "获取指定日期总收入")
    @GetMapping("/revenue/day")
    public Result<Double> getDayTotalRevenue(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        if (date == null)
            date = LocalDate.now();
        Double revenue = statisticsService.getDayTotalRevenue(date);
        return Result.success(revenue);
    }

    @Operation(summary = "获取热销商品榜单")
    @GetMapping("/hot-products")
    public Result<List<Map<String, Object>>> getHotProducts(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "10") Integer limit) {
        if (startDate == null)
            startDate = LocalDate.now();
        if (endDate == null)
            endDate = LocalDate.now();
        List<Map<String, Object>> data = statisticsService.getHotProducts(startDate, endDate, limit);
        return Result.success(data);
    }
}
