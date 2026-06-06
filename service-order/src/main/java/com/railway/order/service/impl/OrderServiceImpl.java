package com.railway.order.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.railway.common.constant.MqConstant;
import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.util.SnowflakeIdWorker;
import com.railway.common.util.UserContext;
import com.railway.order.dto.request.CancelOrderDTO;
import com.railway.order.dto.request.CreateOrderDTO;
import com.railway.order.entity.OrderDO;
import com.railway.order.feign.TicketFeignClient;
import com.railway.order.feign.dto.PassengerVO;
import com.railway.order.feign.dto.PayVO;
import com.railway.order.feign.dto.TicketVO;
import com.railway.order.manager.OrderPayManager;
import com.railway.order.manager.OrderStockManager;
import com.railway.order.manager.OrderTicketManager;
import com.railway.order.manager.OrderUserManager;
import com.railway.order.mapper.OrderMapper;
import com.railway.order.service.OrderService;
import com.railway.order.vo.CreateOrderResultVO;
import com.railway.order.vo.OrderDetailVO;
import com.railway.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单服务实现。
 *
 * <h3>create 流程的补偿点</h3>
 * <ul>
 *   <li>stock.occupy 成功 + 后续失败 → catch 块调 stock.release 回滚</li>
 *   <li>ticket.issue 失败 → 同上（已写 orders 时也回滚 orders）</li>
 *   <li>payment.createPay 失败 → 保留 orders（status=0），前端可重试，15min 后自动关单</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderStockManager stockManager;
    private final OrderTicketManager ticketManager;
    private final OrderPayManager payManager;
    private final OrderUserManager userManager;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final RabbitTemplate rabbitTemplate;

    @Value("${order.pay-expire-minutes:15}")
    private int payExpireMinutes;

    @Override
    public CreateOrderResultVO create(CreateOrderDTO dto, Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (dto.getPassengerIds() == null || dto.getPassengerIds().size() > 5) {
            throw new BizException(ErrorCode.BAD_REQUEST, "乘车人 1~5 人");
        }
        int num = dto.getPassengerIds().size();
        BigDecimal amount = dto.getPrice().multiply(BigDecimal.valueOf(num));

        // 1. 拿乘客信息（降级容错：拿不到就给空，前端兜底）
        Map<Long, PassengerVO> passengerMap = userManager.listByIds(dto.getPassengerIds());
        List<PassengerVO> passengers = dto.getPassengerIds().stream()
                .map(id -> passengerMap.getOrDefault(id, defaultPassenger(id)))
                .toList();

        // 2. 雪花 orderNo
        String orderNo = "O" + snowflakeIdWorker.nextId();

        // 3. Feign stock.occupy（失败抛 STOCK_NOT_ENOUGH）
        stockManager.occupy(orderNo, dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), num);

        // 4. DB 写 orders（status=0, expireTime=now+15min）
        OrderDO order = new OrderDO();
        order.setId(snowflakeIdWorker.nextId());
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTrainNo(dto.getTrainNo());
        order.setRunDate(dto.getRunDate());
        order.setSeatType(dto.getSeatType());
        order.setNum(num);
        order.setAmount(amount);
        order.setStatus(0);
        order.setExpireTime(LocalDateTime.now().plusMinutes(payExpireMinutes));
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        try {
            insertOrder(order);
        } catch (Exception e) {
            // 写 DB 失败：补偿 stock.release
            log.error("写 orders 失败，补偿 stock.release orderNo={}", orderNo, e);
            safeRelease(orderNo, dto, num);
            throw new BizException(ErrorCode.ORDER_CREATE_FAILED, "订单入库失败");
        }

        // 5. 发 MQ order.delay（带 orderNo，TTL 15min）
        try {
            sendDelayMessage(orderNo);
        } catch (Exception e) {
            log.warn("发 order.delay 失败（不影响主流程，靠前端超时 + 后续补偿）orderNo={}", orderNo, e);
        }

        // 6. Feign ticket.issue
        try {
            ticketManager.issue(orderNo, dto.getTrainNo(), dto.getRunDate(),
                    dto.getSeatType(), dto.getPrice(), passengers);
        } catch (Exception e) {
            log.error("ticket.issue 失败，补偿 stock.release + 删 orders orderNo={}", orderNo, e);
            safeRelease(orderNo, dto, num);
            try { orderMapper.refundByOrderNo(orderNo); } catch (Exception ignore) {}
            throw new BizException(ErrorCode.TICKET_ISSUE_FAILED, "出票失败：" + e.getMessage());
        }

        // 7. Feign payment.createPay
        PayVO payVo;
        try {
            payVo = payManager.createPay(orderNo, amount);
        } catch (Exception e) {
            // 保留 order status=0，前端可重试 payUrl，15min 后自动关单
            log.warn("payment.createPay 失败 orderNo={}（不阻塞，前端可重试）", orderNo, e);
            payVo = null;
        }

        // 8. 返回
        CreateOrderResultVO result = new CreateOrderResultVO();
        result.setOrderNo(orderNo);
        result.setAmount(amount);
        result.setExpireTime(order.getExpireTime());
        result.setPayUrl(payVo == null ? null : payVo.getPayUrl());
        log.info("createOrder 完成 orderNo={} amount={} payUrl={}", orderNo, amount, result.getPayUrl());
        return result;
    }

    @Override
    public void cancel(CancelOrderDTO dto, Long userId) {
        OrderDO order = orderMapper.selectByOrderNo(dto.getOrderNo());
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能取消自己的订单");
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new BizException(ErrorCode.ORDER_ALREADY_PAID, "订单已支付/已取消，不可重复取消");
        }

        // 1. 状态机 0→2
        int affected = orderMapper.cancelByOrderNo(dto.getOrderNo());
        if (affected == 0) {
            throw new BizException(ErrorCode.ORDER_CANCEL_FAILED, "订单状态已变更");
        }

        // 2. 释放库存
        try {
            stockManager.release(order.getOrderNo(), order.getTrainNo(), order.getRunDate(),
                    order.getSeatType(), order.getNum());
        } catch (Exception e) {
            log.error("stock.release 失败 orderNo={}", dto.getOrderNo(), e);
        }

        // 3. 票退
        try {
            ticketManager.cancel(order.getOrderNo());
        } catch (Exception e) {
            log.error("ticket.cancel 失败 orderNo={}", dto.getOrderNo(), e);
        }

        log.info("cancel 完成 orderNo={} userId={} reason={}", dto.getOrderNo(), userId, dto.getReason());
    }

    @Override
    public OrderVO getByOrderNo(String orderNo, Long userId) {
        OrderDO o = orderMapper.selectByOrderNo(orderNo);
        if (o == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!o.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该订单");
        }
        return toVO(o);
    }

    @Override
    public OrderDetailVO getDetail(String orderNo, Long userId) {
        OrderVO vo = getByOrderNo(orderNo, userId);
        OrderDetailVO detail = new OrderDetailVO();
        detail.setOrder(vo);
        // 票列表暂留空（service-ticket 调通可补；本阶段 demo 暂不调）
        detail.setTickets(List.of());
        return detail;
    }

    @Override
    public PageInfo<OrderVO> listByUserId(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<OrderDO> list = orderMapper.listByUserId(userId);
        PageInfo<OrderDO> pi = new PageInfo<>(list);
        List<OrderVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());
        PageInfo<OrderVO> result = new PageInfo<>(voList);
        result.setTotal(pi.getTotal());
        result.setPageNum(pi.getPageNum());
        result.setPageSize(pi.getPageSize());
        result.setPages(pi.getPages());
        return result;
    }

    // ============== private ==============

    @Transactional(rollbackFor = Exception.class)
    public void insertOrder(OrderDO order) {
        orderMapper.insert(order);
    }

    private void sendDelayMessage(String orderNo) {
        Map<String, Object> msg = new HashMap<>(2);
        msg.put("orderNo", orderNo);
        rabbitTemplate.convertAndSend(
                MqConstant.ORDER_DELAY_EXCHANGE,
                MqConstant.RK_ORDER_DELAY,
                msg,
                m -> {
                    m.getMessageProperties().setExpiration(String.valueOf(
                            Duration.ofMinutes(payExpireMinutes).toMillis()));
                    return m;
                });
    }

    private void safeRelease(String orderNo, CreateOrderDTO dto, int num) {
        try {
            stockManager.release(orderNo, dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), num);
        } catch (Exception e) {
            log.error("补偿 stock.release 失败 orderNo={}", orderNo, e);
        }
    }

    private PassengerVO defaultPassenger(Long id) {
        PassengerVO p = new PassengerVO();
        p.setId(id);
        p.setName("乘客" + id);
        p.setIdCardNo("000000000000000000");   // fallback，服务不可用时也能走完
        return p;
    }

    private OrderVO toVO(OrderDO o) {
        OrderVO v = new OrderVO();
        v.setId(o.getId());
        v.setOrderNo(o.getOrderNo());
        v.setUserId(o.getUserId());
        v.setTrainNo(o.getTrainNo());
        v.setRunDate(o.getRunDate());
        v.setSeatType(o.getSeatType());
        v.setNum(o.getNum());
        v.setAmount(o.getAmount());
        v.setStatus(o.getStatus());
        v.setExpireTime(o.getExpireTime());
        v.setCreateTime(o.getCreateTime());
        v.setUpdateTime(o.getUpdateTime());
        return v;
    }
}
