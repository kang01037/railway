package com.railway.order.consumer;

import com.railway.common.constant.MqConstant;
import com.railway.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 监听 {@code order.paid.queue}。
 * <p>支付成功后（MQ 降级路径）：调用 orderService.confirmByOrderNo（含缓存清除 + 出票）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = MqConstant.ORDER_PAID_QUEUE)
    public void onOrderPaid(Map<String, Object> msg) {
        String orderNo = (String) msg.get("orderNo");
        Boolean success = (Boolean) msg.get("success");
        if (orderNo == null) {
            log.warn("order.paid 消息无 orderNo 字段 msg={}", msg);
            return;
        }
        log.info("收到 order.paid orderNo={} success={}", orderNo, success);

        if (!Boolean.TRUE.equals(success)) {
            log.info("支付失败 / 取消，不改订单状态 orderNo={}", orderNo);
            return;
        }

        // 走 service 层：状态机 0→1 + 出票 + 缓存清除
        try {
            int affected = orderService.confirmByOrderNo(orderNo);
            log.info("order.paid 处理完成 orderNo={} affected={}", orderNo, affected);
        } catch (Exception e) {
            log.error("order.paid 处理失败 orderNo={}", orderNo, e);
        }
    }
}
