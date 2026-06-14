package com.railway.ticket;

import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 票务服务启动入口（端口 9103）。
 * <p>主要被 service-order 通过 Feign 调用（{@code /tickets/issue|confirm|cancel}），
 * 用户也可经 gateway 查自己订单的票。
 */
@SpringBootApplication(exclude = {MybatisAutoConfiguration.class})
@EnableDiscoveryClient
public class TicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketApplication.class, args);
    }
}
