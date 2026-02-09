package com.ddhj.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ddhj.entity.Order;
import com.ddhj.entity.Product;
import com.ddhj.mapper.OrderMapper;
import com.ddhj.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ProductMapper productMapper;

    /**
     * 获取收入统计
     * 
     * @param date 日期
     * @param mode 模式：hour(分时), day(日), month(月)
     */
    public Map<String, Object> getRevenueStatistics(LocalDate date, String mode) {
        Map<String, Object> result = new HashMap<>();

        if ("hour".equals(mode)) {
            // 分时统计 (0:00-23:00)
            List<Map<String, Object>> hourlyData = new ArrayList<>();
            for (int hour = 0; hour <= 23; hour++) {
                LocalDateTime startTime = LocalDateTime.of(date, LocalTime.of(hour, 0));
                LocalDateTime endTime = LocalDateTime.of(date, LocalTime.of(hour, 59, 59));

                Double revenue = getRevenueBetween(startTime, endTime);
                Map<String, Object> item = new HashMap<>();
                item.put("time", hour + ":00");
                item.put("revenue", revenue);
                hourlyData.add(item);
            }
            result.put("data", hourlyData);
        } else if ("day".equals(mode)) {
            // 日统计 (当月每一天)
            List<Map<String, Object>> dailyData = new ArrayList<>();
            int daysInMonth = date.lengthOfMonth();
            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate currentDate = LocalDate.of(date.getYear(), date.getMonth(), day);
                LocalDateTime startTime = currentDate.atStartOfDay();
                LocalDateTime endTime = currentDate.atTime(23, 59, 59);

                Double revenue = getRevenueBetween(startTime, endTime);
                Map<String, Object> item = new HashMap<>();
                item.put("time", day + "日");
                item.put("revenue", revenue);
                dailyData.add(item);
            }
            result.put("data", dailyData);
        } else if ("month".equals(mode)) {
            // 月统计 (当年每个月)
            List<Map<String, Object>> monthlyData = new ArrayList<>();
            for (int month = 1; month <= 12; month++) {
                LocalDate firstDay = LocalDate.of(date.getYear(), month, 1);
                LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
                LocalDateTime startTime = firstDay.atStartOfDay();
                LocalDateTime endTime = lastDay.atTime(23, 59, 59);

                Double revenue = getRevenueBetween(startTime, endTime);
                Map<String, Object> item = new HashMap<>();
                item.put("time", month + "月");
                item.put("revenue", revenue);
                monthlyData.add(item);
            }
            result.put("data", monthlyData);
        }

        return result;
    }

    /**
     * 获取指定日期总收入
     */
    public Double getDayTotalRevenue(LocalDate date) {
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.atTime(23, 59, 59);
        return getRevenueBetween(startTime, endTime);
    }

    /**
     * 获取热销商品榜单
     */
    public List<Map<String, Object>> getHotProducts(LocalDate startDate, LocalDate endDate, Integer limit) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Order::getOrderDate, startDateTime)
                .le(Order::getOrderDate, endDateTime);
        List<Order> orders = orderMapper.selectList(wrapper);

        // 统计商品销量
        Map<Long, Integer> productSalesMap = new HashMap<>();
        for (Order order : orders) {
            Map<String, Object> items = order.getItems();
            if (items != null) {
                for (Map.Entry<String, Object> entry : items.entrySet()) {
                    String productIdStr = entry.getKey();
                    if ("notes".equals(productIdStr))
                        continue;

                    try {
                        Long productId = Long.parseLong(productIdStr);
                        Object value = entry.getValue();

                        if (value instanceof List) {
                            List<?> itemList = (List<?>) value;
                            for (Object item : itemList) {
                                if (item instanceof Map) {
                                    Map<?, ?> itemMap = (Map<?, ?>) item;
                                    Object quantityObj = itemMap.get("购买数量");
                                    if (quantityObj != null) {
                                        Integer quantity = Integer.parseInt(quantityObj.toString());
                                        productSalesMap.put(productId,
                                                productSalesMap.getOrDefault(productId, 0) + quantity);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }
        }

        // 排序并获取Top N
        List<Map.Entry<Long, Integer>> sortedList = productSalesMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());

        // 获取商品详情
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : sortedList) {
            Product product = productMapper.selectById(entry.getKey());
            if (product != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("productId", product.getId());
                item.put("productName", product.getName());
                item.put("categoryId", product.getCategoryId());
                item.put("images", product.getImages());
                item.put("sellPrice", product.getSellPrice());
                item.put("sales", entry.getValue());
                result.add(item);
            }
        }

        return result;
    }

    private Double getRevenueBetween(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Order::getOrderDate, startTime)
                .le(Order::getOrderDate, endTime);
        List<Order> orders = orderMapper.selectList(wrapper);

        return orders.stream()
                .mapToDouble(Order::getPaidAmount)
                .sum();
    }
}
