package com.railway.order.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.common.constant.MqConstant;
import com.railway.common.constant.RedisKeyConstant;
import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.util.SnowflakeIdWorker;
import com.railway.common.util.UserContext;
import com.railway.order.dto.request.CancelOrderDTO;
import com.railway.order.dto.request.CreateOrderDTO;
import com.railway.order.entity.OrderDO;
import com.railway.order.feign.dto.PassengerVO;
import com.railway.order.feign.dto.PayVO;
import com.railway.order.feign.StockFeignClient;
import com.railway.order.feign.dto.ConfirmStockDTO;
import com.railway.order.feign.dto.OccupyStockDTO;
import com.railway.order.feign.dto.ReleaseStockDTO;
import com.railway.order.manager.OrderPayManager;
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
 * <h3>create 流程</h3>
 * <ol>
 *   <li>ticket.preAllocate — Redis SPOP 预占座位（统一库存管理）</li>
 *   <li>DB 写 orders（status=0）</li>
 *   <li>payment.createPay — 生成支付链接</li>
 *   <li>支付成功 → confirmByOrderNo → ticket.issue 写 MySQL ticket 表</li>
 * </ul>
 * <h3>补偿点</h3>
 * <ul>
 *   <li>DB 写入失败 → ticket.cancel 归还座位池</li>
 *   <li>ticket.issue 失败 → 回滚订单状态 1→0</li>
 *   <li>payment.createPay 失败 → 保留 orders（status=0），15min 后自动关单</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderTicketManager ticketManager;
    private final OrderPayManager payManager;
    private final OrderUserManager userManager;
    private final StockFeignClient stockFeignClient;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final RabbitTemplate rabbitTemplate;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

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

        // 1. 雪花 orderNo
        String orderNo = "O" + snowflakeIdWorker.nextId();

        // 2. 先写订单（status=4 排队中），让前端可以轮询
        OrderDO order = new OrderDO();
        order.setId(snowflakeIdWorker.nextId());
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTrainNo(dto.getTrainNo());
        order.setRunDate(dto.getRunDate());
        order.setSeatType(dto.getSeatType());
        order.setNum(num);
        order.setAmount(amount);
        order.setStatus(4);  // 排队中
        order.setExpireTime(LocalDateTime.now().plusMinutes(payExpireMinutes));
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        try {
            insertOrder(order);
        } catch (Exception e) {
            log.error("写 orders 失败 orderNo={}", orderNo, e);
            throw new BizException(ErrorCode.ORDER_CREATE_FAILED, "订单创建失败");
        }

        // 3. 发送 MQ 消息（削峰填谷）
        Map<String, Object> msg = new HashMap<>();
        msg.put("orderNo", orderNo);
        msg.put("userId", userId);
        msg.put("trainNo", dto.getTrainNo());
        msg.put("runDate", dto.getRunDate().toString());
        msg.put("seatType", dto.getSeatType());
        msg.put("num", num);
        msg.put("price", dto.getPrice());
        msg.put("amount", amount);
        msg.put("passengerIds", dto.getPassengerIds());  // 添加乘客ID列表
        try {
            rabbitTemplate.convertAndSend(MqConstant.ORDER_EXCHANGE, MqConstant.RK_ORDER_CREATE, msg);
            log.info("发送订单创建消息 orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("发送 MQ 失败，删除订单 orderNo={}", orderNo, e);
            orderMapper.deleteByOrderNo(orderNo);
            throw new BizException(ErrorCode.ORDER_CREATE_FAILED, "订单创建失败");
        }

        // 4. 返回排队中状态
        CreateOrderResultVO result = new CreateOrderResultVO();
        result.setOrderNo(orderNo);
        result.setStatus(4);  // 排队中
        result.setAmount(amount);
        result.setExpireTime(order.getExpireTime());
        log.info("createOrder 排队中 orderNo={} amount={}", orderNo, amount);
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

        // 2. 归还座位池（ticket.cancel 会归还 Redis 座位 + 清理预占数据）
        try {
            ticketManager.cancel(order.getOrderNo());
        } catch (Exception e) {
            log.error("ticket.cancel 失败 orderNo={}", dto.getOrderNo(), e);
        }

        // 3. 释放库存（stock.release 归还 train_seat_stock.remain）
        safeReleaseStock(order.getOrderNo(), order.getTrainNo(), order.getRunDate(), order.getSeatType(), order.getNum());

        // 4. 清除订单缓存
        evictOrderCache(dto.getOrderNo(), order.getUserId());

        log.info("cancel 完成 orderNo={} userId={} reason={}", dto.getOrderNo(), userId, dto.getReason());
    }

    /** 缓存 TTL：5 分钟 */
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    @Override
    public OrderVO getByOrderNo(String orderNo, Long userId) {
        // 1. 查 Redis 缓存
        String cacheKey = RedisKeyConstant.orderByOrderNoKey(orderNo);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                OrderVO vo = objectMapper.readValue(cached, OrderVO.class);
                if (vo != null && vo.getUserId().equals(userId)) {
                    return vo;
                }
            } catch (JsonProcessingException e) {
                log.warn("订单缓存解析失败，回源查询 orderNo={}", orderNo, e);
            }
        }

        // 2. 回源 MySQL
        OrderDO o = orderMapper.selectByOrderNo(orderNo);
        if (o == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!o.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该订单");
        }
        OrderVO vo = toVO(o);

        // 3. 写入 Redis 缓存
        try {
            stringRedisTemplate.opsForValue().set(cacheKey,
                    objectMapper.writeValueAsString(vo), CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("订单缓存写入失败 orderNo={}", orderNo, e);
        }
        return vo;
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
        // 1. 查 Redis 缓存
        String cacheKey = RedisKeyConstant.orderByUserIdKey(userId, pageNum);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<PageInfo<OrderVO>>() {});
            } catch (JsonProcessingException e) {
                log.warn("订单列表缓存解析失败，回源查询 userId={}", userId, e);
            }
        }

        // 2. 回源 MySQL
        PageHelper.startPage(pageNum, pageSize);
        List<OrderDO> list = orderMapper.listByUserId(userId);
        PageInfo<OrderDO> pi = new PageInfo<>(list);
        List<OrderVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());
        PageInfo<OrderVO> result = new PageInfo<>(voList);
        result.setTotal(pi.getTotal());
        result.setPageNum(pi.getPageNum());
        result.setPageSize(pi.getPageSize());
        result.setPages(pi.getPages());

        // 3. 写入 Redis 缓存
        try {
            stringRedisTemplate.opsForValue().set(cacheKey,
                    objectMapper.writeValueAsString(result), CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("订单列表缓存写入失败 userId={}", userId, e);
        }
        return result;
    }

    @Override
    public int confirmByOrderNo(String orderNo) {
        // 1. 状态机 0→1
        int affected = orderMapper.confirmByOrderNo(orderNo);
        if (affected == 0) {
            log.info("order 状态非 0，跳过确认 orderNo={}", orderNo);
            return 0;
        }

        // 2. 查 order 拿 runDate
        OrderDO order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.error("confirmByOrderNo 后查不到 order orderNo={}", orderNo);
            return affected;
        }

        // 3. 调 ticket.issue（更新 ticket status 0→1）
        try {
            ticketManager.issue(orderNo, order.getRunDate());
        } catch (Exception e) {
            // ticket 数据已在 preAllocate 时写入 MySQL，issue 只是状态更新
            // 失败不影响订单状态，可后续补偿
            log.error("ticket.issue 失败（ticket 数据已存在，可补偿）orderNo={}", orderNo, e);
        }

        // 4. 调 stock.confirm（确认库存扣减）
        safeConfirmStock(orderNo, order.getTrainNo(), order.getRunDate(), order.getSeatType(), order.getNum());

        // 清除订单缓存（状态已变更）
        evictOrderCache(orderNo, order.getUserId());
        log.info("confirmByOrderNo 完成 orderNo={}", orderNo);
        return affected;
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

    private void safeCancelTicket(String orderNo) {
        try {
            ticketManager.cancel(orderNo);
        } catch (Exception e) {
            log.error("补偿 ticket.cancel 失败 orderNo={}", orderNo, e);
        }
    }

    private void safeReleaseStock(String orderNo, String trainNo, java.time.LocalDate runDate,
                                  String seatType, int num) {
        try {
            ReleaseStockDTO dto = new ReleaseStockDTO();
            dto.setOrderNo(orderNo);
            dto.setTrainNo(trainNo);
            dto.setRunDate(runDate);
            dto.setSeatType(seatType);
            dto.setNum(num);
            var r = stockFeignClient.release(dto);
            if (r == null || r.getCode() != 200) {
                log.error("补偿 stock.release 失败 orderNo={} msg={}", orderNo,
                        r == null ? "null" : r.getMessage());
            }
        } catch (Exception e) {
            log.error("补偿 stock.release 异常 orderNo={}", orderNo, e);
        }
    }

    private void safeConfirmStock(String orderNo, String trainNo, java.time.LocalDate runDate,
                                  String seatType, int num) {
        try {
            ConfirmStockDTO dto = new ConfirmStockDTO();
            dto.setOrderNo(orderNo);
            dto.setTrainNo(trainNo);
            dto.setRunDate(runDate);
            dto.setSeatType(seatType);
            dto.setNum(num);
            var r = stockFeignClient.confirm(dto);
            if (r == null || r.getCode() != 200) {
                log.error("stock.confirm 失败 orderNo={} msg={}", orderNo,
                        r == null ? "null" : r.getMessage());
            }
        } catch (Exception e) {
            log.error("stock.confirm 异常 orderNo={}", orderNo, e);
        }
    }

    /** 清除订单相关缓存 */
    private void evictOrderCache(String orderNo, Long userId) {
        try {
            stringRedisTemplate.delete(RedisKeyConstant.orderByOrderNoKey(orderNo));
            // 清除用户订单列表缓存（简单实现：清除前3页）
            for (int i = 1; i <= 3; i++) {
                stringRedisTemplate.delete(RedisKeyConstant.orderByUserIdKey(userId, i));
            }
        } catch (Exception e) {
            log.warn("清除订单缓存失败 orderNo={}", orderNo, e);
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
