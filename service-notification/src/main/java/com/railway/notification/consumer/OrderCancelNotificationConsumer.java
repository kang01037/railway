package com.railway.notification.consumer;

import com.railway.common.constant.MqConstant;
import com.railway.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 监听 {@code order.cancel.queue}（来自 {@code order.delay.queue} 的 DLX 推送），发订单取消通知。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelNotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = MqConstant.ORDER_CANCEL_QUEUE)
    public void onMessage(Map<String, Object> msg) {
        log.debug("收到 order.cancel msg={}", msg);
        try {
            notificationService.onOrderCancel(msg);
        } catch (Exception e) {
            log.error("处理 order.cancel 失败 msg={}", msg, e);
            throw e;
        }
    }
}
