package com.railway.notification.service;

import com.railway.notification.entity.NotificationLogDO;

import java.util.Map;

/**
 * 通知服务：把 MQ 消息翻译成"通知"，落库 + 模拟发送。
 *
 * <p>Demo 阶段：channel 固定为 LOG，receiver / content 由调用方传入。
 */
public interface NotificationService {

    /**
     * 处理 {@code order.paid} 事件 → 发支付成功通知。
     * @param msg MQ body：{orderNo, payNo, amount, success}
     */
    void onOrderPaid(Map<String, Object> msg);

    /**
     * 处理 {@code order.cancel} 事件 → 发订单取消通知。
     * @param msg MQ body：{orderNo}
     */
    void onOrderCancel(Map<String, Object> msg);

    /**
     * 持久化一条通知日志。
     */
    void saveLog(NotificationLogDO entity);
}
