package com.ddhj.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.ddhj.mapper")
public class MybatisPlusConfig {

}
