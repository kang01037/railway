package com.railway.order.consumer;

import com.railway.common.constant.MqConstant;
import com.railway.common.constant.RedisKeyConstant;
import com.railway.order.feign.StockFeignClient;
import com.railway.order.feign.dto.OccupyStockDTO;
import com.railway.order.feign.dto.PassengerVO;
import com.railway.order.feign.dto.PayVO;
import com.railway.order.manager.OrderPayManager;
import com.railway.order.manager.OrderTicketManager;
import com.railway.order.manager.OrderUserManager;
import com.railway.order.mapper.OrderMapper;
import com.railway.order.entity.OrderDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单创建消费者（MQ 削峰填谷）。
 * <p>异步处理订单创建：扣库存 → 写 ticket → 更新订单状态 → 生成支付链接。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreateConsumer {

    private final OrderMapper orderMapper;
    private final OrderTicketManager ticketManager;
    private final OrderPayManager payManager;
    private final OrderUserManager userManager;
    private final StockFeignClient stockFeignClient;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = MqConstant.ORDER_CREATE_QUEUE)
    public void onOrderCreate(Map<String, Object> msg) {
        String orderNo = (String) msg.get("orderNo");
        if (orderNo == null) {
            log.warn("order.create 消息无 orderNo 字段 msg={}", msg);
            return;
        }
        log.info("收到 order.create 消息 orderNo={}", orderNo);

        // 1. 查询订单（status=4 排队中）
        OrderDO order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.error("order.create 查不到订单 orderNo={}", orderNo);
            return;
        }
        if (order.getStatus() == null || order.getStatus() != 4) {
            log.info("订单状态非 4（排队中），跳过处理 orderNo={} status={}", orderNo, order.getStatus());
            return;
        }

        // 2. 解析消息参数（MQ 反序列化后数字可能为 String，需安全转换）
        Long userId = Long.valueOf(msg.get("userId").toString());
        String trainNo = (String) msg.get("trainNo");
        LocalDate runDate = LocalDate.parse((String) msg.get("runDate"));
        String seatType = (String) msg.get("seatType");
        int num = Integer.parseInt(msg.get("num").toString());
        BigDecimal price = new BigDecimal(msg.get("price").toString());
        BigDecimal amount = new BigDecimal(msg.get("amount").toString());

        // 3. 拿乘客信息（JSON 反序列化后 List 元素为 Integer，需转换）
        List<?> rawPassengerIds = (List<?>) msg.get("passengerIds");
        if (rawPassengerIds == null) {
            log.error("order.create 消息无 passengerIds orderNo={}", orderNo);
            updateOrderStatus(orderNo, 2);  // 已取消
            return;
        }
        List<Long> passengerIds = rawPassengerIds.stream()
                .map(id -> Long.valueOf(id.toString()))
                .toList();
        Map<Long, PassengerVO> passengerMap = userManager.listByIds(passengerIds);
        List<PassengerVO> passengers = passengerIds.stream()
                .map(id -> passengerMap.getOrDefault(id, defaultPassenger(id)))
                .toList();

        // 4. Feign ticket.preAllocate（Redis SPOP 预占座位 + 写 MySQL ticket 表）
        try {
            ticketManager.preAllocate(orderNo, trainNo, runDate, seatType, price, passengers);
        } catch (Exception e) {
            log.error("ticket.preAllocate 失败 orderNo={}", orderNo, e);
            updateOrderStatus(orderNo, 2);  // 已取消
            return;
        }

        // 5. Feign stock.occupy（扣减 train_seat_stock.remain 余票数量）
        try {
            OccupyStockDTO occupyDTO = new OccupyStockDTO();
            occupyDTO.setOrderNo(orderNo);
            occupyDTO.setTrainNo(trainNo);
            occupyDTO.setRunDate(runDate);
            occupyDTO.setSeatType(seatType);
            occupyDTO.setNum(num);
            var r = stockFeignClient.occupy(occupyDTO);
            if (r == null || r.getCode() != 200) {
                log.error("stock.occupy 失败 orderNo={} msg={}", orderNo,
                        r == null ? "无响应" : r.getMessage());
                safeCancelTicket(orderNo);
                updateOrderStatus(orderNo, 2);  // 已取消
                return;
            }
        } catch (Exception e) {
            log.error("stock.occupy 异常 orderNo={}", orderNo, e);
            safeCancelTicket(orderNo);
            updateOrderStatus(orderNo, 2);  // 已取消
            return;
        }

        // 6. 更新订单状态 4→0（待支付）
        int affected = orderMapper.updateStatus(orderNo, 4, 0);
        if (affected == 0) {
            log.info("订单状态已变更，跳过处理 orderNo={}", orderNo);
            safeCancelTicket(orderNo);
            safeReleaseStock(orderNo, trainNo, runDate, seatType, num);
            return;
        }

        // 7. 发 MQ order.delay（带 orderNo，TTL 15min）
        try {
            sendDelayMessage(orderNo);
        } catch (Exception e) {
            log.warn("发 order.delay 失败 orderNo={}", orderNo, e);
        }

        // 8. Feign payment.createPay
        try {
            PayVO payVo = payManager.createPay(orderNo, amount);
            if (payVo != null) {
                // 将 payUrl 存入 Redis（前端轮询时获取）
                String payUrlKey = RedisKeyConstant.ORDER_PAY_URL + orderNo;
                stringRedisTemplate.opsForValue().set(payUrlKey, payVo.getPayUrl(), Duration.ofMinutes(30));
                log.info("订单创建完成 orderNo={} payUrl={}", orderNo, payVo.getPayUrl());
            }
        } catch (Exception e) {
            log.warn("payment.createPay 失败 orderNo={}", orderNo, e);
        }

        log.info("order.create 处理完成 orderNo={}", orderNo);
    }

    private void updateOrderStatus(String orderNo, int status) {
        try {
            orderMapper.updateStatus(orderNo, 4, status);
        } catch (Exception e) {
            log.error("更新订单状态失败 orderNo={} status={}", orderNo, status, e);
        }
    }

    private void safeCancelTicket(String orderNo) {
        try {
            ticketManager.cancel(orderNo);
        } catch (Exception e) {
            log.error("补偿 ticket.cancel 失败 orderNo={}", orderNo, e);
        }
    }

    private void safeReleaseStock(String orderNo, String trainNo, LocalDate runDate,
                                  String seatType, int num) {
        try {
            var dto = new com.railway.order.feign.dto.ReleaseStockDTO();
            dto.setOrderNo(orderNo);
            dto.setTrainNo(trainNo);
            dto.setRunDate(runDate);
            dto.setSeatType(seatType);
            dto.setNum(num);
            stockFeignClient.release(dto);
        } catch (Exception e) {
            log.error("补偿 stock.release 失败 orderNo={}", orderNo, e);
        }
    }

    private void sendDelayMessage(String orderNo) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("orderNo", orderNo);
        rabbitTemplate.convertAndSend(
                MqConstant.ORDER_DELAY_EXCHANGE,
                MqConstant.RK_ORDER_DELAY,
                msg,
                m -> {
                    m.getMessageProperties().setExpiration(String.valueOf(
                            Duration.ofMinutes(15).toMillis()));
                    return m;
                });
    }

    private PassengerVO defaultPassenger(Long id) {
        PassengerVO vo = new PassengerVO();
        vo.setId(id);
        vo.setName("乘客" + id);
        vo.setIdCardNo("000000000000000000");
        return vo;
    }
}
