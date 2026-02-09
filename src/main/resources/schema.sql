-- 创建数据库
CREATE DATABASE IF NOT EXISTS ddhj DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ddhj;

-- 分类表
CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(100) NOT NULL COMMENT '分类名称',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除(0:未删除,1:删除)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_is_delete` (`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- 商品表
CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `images` JSON COMMENT '图片路径数组',
    `sell_price` DECIMAL(10,2) NOT NULL COMMENT '出售价格',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `purchase_price` DECIMAL(10,2) COMMENT '进货价格',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除(0:未删除,1:删除)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_is_delete` (`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 规格表
CREATE TABLE IF NOT EXISTS `specification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规格ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `name` VARCHAR(200) NOT NULL COMMENT '规格名称',
    `specs` JSON COMMENT '规格键值对',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除(0:未删除,1:删除)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_is_delete` (`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规格表';

-- 订单表
CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_number` VARCHAR(50) NOT NULL COMMENT '唯一订单号',
    `items` JSON NOT NULL COMMENT '订单商品详情',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    `paid_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    `notes` TEXT COMMENT '备注信息',
    `order_date` DATETIME NOT NULL COMMENT '下单日期',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除(0:未删除,1:删除)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_number` (`order_number`),
    INDEX `idx_order_date` (`order_date`),
    INDEX `idx_is_delete` (`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';
