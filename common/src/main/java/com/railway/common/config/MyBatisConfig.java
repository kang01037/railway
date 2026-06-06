package com.railway.common.config;

import com.github.pagehelper.PageInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis + PageHelper 配置：
 * <ul>
 *   <li>{@code @MapperScan} 扫描所有业务模块的 mapper 接口</li>
 *   <li>注册 {@link PageInterceptor}（由 pagehelper-spring-boot-starter 自动装配时优先使用）</li>
 *   <li>createTime / updateTime 由 Service 层 {@code new Date()} 手动写入</li>
 * </ul>
 */
@Configuration
@MapperScan("com.railway.**.mapper")
public class MyBatisConfig {

    @Bean
    @ConditionalOnMissingBean
    public PageInterceptor pageInterceptor() {
        return new PageInterceptor();
    }
}
