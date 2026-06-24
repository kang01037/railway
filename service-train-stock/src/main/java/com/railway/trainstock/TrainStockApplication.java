package com.railway.trainstock;

import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 车次 + 库存服务启动入口（端口 9102）。
 *
 * <p>本服务不主动调用其他服务（出站 Feign 暂不开）；后续 service-order 调
 * {@code /stock/occupy} / {@code /stock/release} 等内部接口（标 {@code @AuthIgnore}）。
 */
@SpringBootApplication(exclude = {MybatisAutoConfiguration.class})
@EnableDiscoveryClient
@EnableScheduling
public class TrainStockApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainStockApplication.class, args);
    }
}
