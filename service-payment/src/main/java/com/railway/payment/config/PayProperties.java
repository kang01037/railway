package com.railway.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付相关配置（{@code pay.*}）。
 */
@Data
@ConfigurationProperties(prefix = "pay")
public class PayProperties {

    /**
     * 回调地址前缀（gateway 暴露给前端的 URL）。
     * 例：{@code http://localhost:9000/api/payment/callback}
     */
    private String callbackBaseUrl = "http://localhost:9000/api/payment/pay/callback";
}
