package com.railway.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 支付服务启动入口（端口 9104）。
 * <p>职责：发起支付（模拟） + 接收支付回调 + 发 RabbitMQ {@code order.paid} 给 service-order 消费。
 * <p><b>不消费</b>自己发的消息（避免循环），消费侧在 service-order。
 */
@SpringBootApplication
@EnableDiscoveryClient
public class PaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
