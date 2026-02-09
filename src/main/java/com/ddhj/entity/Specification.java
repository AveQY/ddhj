package com.ddhj.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "specification", autoResultMap = true)
public class Specification {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long productId;
    
    private String name;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> specs;
    
    private Integer stock;
    
    @TableLogic
    private Integer isDelete;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
