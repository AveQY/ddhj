package com.ddhj.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "orders", autoResultMap = true)
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNumber;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> items;

    private Double totalAmount;

    private Double paidAmount;

    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime orderDate;

    @TableLogic
    private Integer isDelete;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
