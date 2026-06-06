package com.railway.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 用户服务启动类。
 *
 * <p>包扫描：默认扫描 {@code com.railway.user} 下所有 @Component / @Service / @Controller。
 * Mapper 扫描由 {@code common} 模块的 MyBatisConfig 提供（{@code @MapperScan("com.railway.**.mapper")}），
 * 故无需在启动类重复声明。
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.railway.user.feign")
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
