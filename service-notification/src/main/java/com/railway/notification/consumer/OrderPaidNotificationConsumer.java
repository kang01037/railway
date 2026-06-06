package com.railway.notification.consumer;

import com.railway.common.constant.MqConstant;
import com.railway.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 监听 {@code order.paid.queue}，发支付成功通知。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidNotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = MqConstant.ORDER_PAID_QUEUE)
    public void onMessage(Map<String, Object> msg) {
        log.debug("收到 order.paid msg={}", msg);
        try {
            notificationService.onOrderPaid(msg);
        } catch (Exception e) {
            log.error("处理 order.paid 失败 msg={}", msg, e);
            // 重试 3 次后进 DLQ（listener.simple.retry 配置），最终落 retry.queue
            throw e;
        }
    }
}
