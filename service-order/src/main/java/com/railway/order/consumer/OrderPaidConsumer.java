package com.railway.order.consumer;

import com.railway.common.constant.MqConstant;
import com.railway.order.manager.OrderStockManager;
import com.railway.order.manager.OrderTicketManager;
import com.railway.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 监听 {@code order.paid.queue}。
 * <p>支付成功后：order 0→1 → 调 stock.confirm → 调 ticket.confirm。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidConsumer {

    private final OrderMapper orderMapper;
    private final OrderStockManager stockManager;
    private final OrderTicketManager ticketManager;

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

        // 1. 状态机 0→1
        int affected = orderMapper.confirmByOrderNo(orderNo);
        if (affected == 0) {
            log.info("order 状态非 0，跳过（可能已确认）orderNo={}", orderNo);
            return;
        }

        // 2. 查 order 拿 train/runDate/seatType/num
        var order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.error("order.paid 后查不到 order orderNo={}", orderNo);
            return;
        }

        // 3. 调 stock.confirm
        try {
            stockManager.confirm(orderNo, order.getTrainNo(), order.getRunDate(),
                    order.getSeatType(), order.getNum());
        } catch (Exception e) {
            log.error("stock.confirm 失败 orderNo={}", orderNo, e);
        }

        // 4. 调 ticket.confirm
        try {
            ticketManager.confirm(orderNo);
        } catch (Exception e) {
            log.error("ticket.confirm 失败 orderNo={}", orderNo, e);
        }
        log.info("order.paid 处理完成 orderNo={}", orderNo);
    }
}
