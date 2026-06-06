package com.railway.notification.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知发送日志（{@code notification_log} 表）。
 *
 * <p>type: 0-支付成功 1-订单取消 2-订单超时关闭
 * <p>channel: LOG（demo 阶段）/ SMS / EMAIL
 * <p>status: 0-成功 1-失败
 */
@Data
public class NotificationLogDO {

    private Long id;
    private String orderNo;
    private Integer type;
    private String channel;
    private String receiver;
    private String content;
    private Integer status;
    private Integer retryCount;
    private String errorMsg;
    private LocalDateTime createTime;
}
