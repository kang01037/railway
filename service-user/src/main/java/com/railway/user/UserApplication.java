package com.railway.user;

import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 用户服务启动类。
 *
 * <p>排除 MybatisAutoConfiguration，由 common 模块的 MyBatisConfig 手动创建 SqlSessionFactory，
 * 确保 mapperLocations 被正确加载。
 */
@SpringBootApplication(exclude = {MybatisAutoConfiguration.class})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.railway.user.feign")
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
