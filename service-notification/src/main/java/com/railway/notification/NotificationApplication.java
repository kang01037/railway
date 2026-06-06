package com.railway.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 通知服务入口。**纯 MQ 消费者**，无 HTTP Controller。
 *
 * <p>监听 {@code order.paid.queue} 与 {@code order.cancel.queue}，模拟发送短信 / 邮件（demo 阶段仅打日志 + 落库）。
 */
@EnableDiscoveryClient
@SpringBootApplication
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
