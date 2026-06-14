package com.railway.order.consumer;

import com.railway.common.constant.MqConstant;
import com.railway.order.feign.StockFeignClient;
import com.railway.order.feign.dto.ReleaseStockDTO;
import com.railway.order.manager.OrderTicketManager;
import com.railway.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 监听 {@code order.cancel.queue}（由 orderDelayQueue TTL 过期后 DLX 推来）。
 * <p>订单 15 分钟未支付：自动关单 = 归还座位池 + 释放库存 + 状态 0→3。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDelayCancelConsumer {

    private final OrderMapper orderMapper;
    private final OrderTicketManager ticketManager;
    private final StockFeignClient stockFeignClient;

    @RabbitListener(queues = MqConstant.ORDER_CANCEL_QUEUE)
    public void onOrderCancel(Map<String, Object> msg) {
        String orderNo = (String) msg.get("orderNo");
        if (orderNo == null) {
            log.warn("order.cancel 消息无 orderNo 字段 msg={}", msg);
            return;
        }
        log.info("收到 order.cancel（延迟关单）orderNo={}", orderNo);

        var order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.warn("order.cancel 查不到 order orderNo={}", orderNo);
            return;
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            log.info("order 状态非 0（已支付/已取消），跳过关单 orderNo={} status={}",
                    orderNo, order.getStatus());
            return;
        }

        // 1. 状态机 0→3（退款/关单）
        int affected = orderMapper.refundByOrderNo(orderNo);
        if (affected == 0) {
            log.info("order 状态已变，跳过关单 orderNo={}", orderNo);
            return;
        }

        // 2. 归还座位池（ticket.cancel 会归还 Redis 座位 + 清理预占数据）
        try {
            ticketManager.cancel(orderNo);
        } catch (Exception e) {
            log.error("ticket.cancel 失败 orderNo={}", orderNo, e);
        }

        // 3. 释放库存（stock.release 归还 train_seat_stock.remain）
        try {
            ReleaseStockDTO dto = new ReleaseStockDTO();
            dto.setOrderNo(orderNo);
            dto.setTrainNo(order.getTrainNo());
            dto.setRunDate(order.getRunDate());
            dto.setSeatType(order.getSeatType());
            dto.setNum(order.getNum());
            var r = stockFeignClient.release(dto);
            if (r == null || r.getCode() != 200) {
                log.error("stock.release 失败 orderNo={} msg={}", orderNo,
                        r == null ? "null" : r.getMessage());
            }
        } catch (Exception e) {
            log.error("stock.release 异常 orderNo={}", orderNo, e);
        }

        log.info("order.cancel 自动关单完成 orderNo={}", orderNo);
    }
}
