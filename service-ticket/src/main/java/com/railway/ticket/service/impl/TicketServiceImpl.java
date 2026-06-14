package com.railway.ticket.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.common.constant.RedisKeyConstant;
import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.util.IdCardValidator;
import com.railway.common.util.SnowflakeIdWorker;
import com.railway.ticket.dto.request.CancelDTO;
import com.railway.ticket.dto.request.IssueByOrderNoDTO;
import com.railway.ticket.dto.request.IssueDTO;
import com.railway.ticket.entity.TicketDO;
import com.railway.ticket.mapper.TicketMapper;
import com.railway.ticket.manager.SeatPoolManager;
import com.railway.ticket.service.TicketService;
import com.railway.ticket.vo.IssueVO;
import com.railway.ticket.vo.TicketVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 票务服务实现。
 *
 * <p>流程：
 * <ol>
 *   <li>下单时：{@link #preAllocate} — Redis SPOP 预占座位 + 写 MySQL ticket 表（status=0 待支付）</li>
 *   <li>支付成功：{@link #issue} — 更新 ticket status 0→1（已出票），不再依赖 Redis 预占数据</li>
 *   <li>取消/超时：{@link #cancel} — 归还 Redis 座位 + 更新 ticket 状态</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketMapper ticketMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final SeatPoolManager seatPoolManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /** 预占座位数据 TTL：30 分钟（远大于支付超时 15 分钟，避免边界过期） */
    private static final Duration RESERVE_TTL = Duration.ofMinutes(30);

    // ==================== 下单时：Redis 预占 + 写 MySQL ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IssueVO preAllocate(IssueDTO dto) {
        // 0. 幂等：同一 orderNo 已有 ticket 记录 → 直接返回
        if (ticketMapper.countByOrderNo(dto.getOrderNo()) > 0) {
            List<TicketDO> exist = ticketMapper.listByOrderNo(dto.getOrderNo());
            log.info("preAllocate 幂等 orderNo={} 已存在 ticket 数={}", dto.getOrderNo(), exist.size());
            return new IssueVO(dto.getOrderNo(), exist.size(),
                    exist.stream().map(TicketDO::getTicketNo).toList());
        }

        // 1. 初始化座位池
        int poolSize = Math.max(20, dto.getPassengers().size() * 8);
        seatPoolManager.initIfAbsent(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), poolSize);

        // 2. SPOP 预占座位
        List<SeatReserveItem> reserves = new ArrayList<>(dto.getPassengers().size());
        for (IssueDTO.PassengerItem p : dto.getPassengers()) {
            String seatNo = seatPoolManager.pop(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType());
            if (seatNo == null) {
                // 池空：回滚已弹出的座位
                for (SeatReserveItem r : reserves) {
                    seatPoolManager.push(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), r.getSeatNo());
                }
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "座位不足");
            }
            SeatReserveItem item = new SeatReserveItem();
            item.setPassengerId(p.getPassengerId());
            item.setPassengerName(p.getPassengerName());
            item.setIdCardNo(p.getIdCardNo());
            item.setSeatNo(seatNo);
            item.setCarriageNo(seatPoolManager.defaultCarriage());
            reserves.add(item);
        }

        // 3. 直接写 MySQL ticket 表（status=0 待支付），同时存 Redis 预占数据（cancel 时归还座位用）
        LocalDateTime now = LocalDateTime.now();
        List<String> ticketNos = new ArrayList<>(reserves.size());
        for (SeatReserveItem item : reserves) {
            TicketDO t = new TicketDO();
            long id = snowflakeIdWorker.nextId();
            t.setId(id);
            t.setTicketNo("T" + id);
            t.setOrderNo(dto.getOrderNo());
            t.setTrainNo(dto.getTrainNo());
            t.setRunDate(dto.getRunDate());
            t.setSeatType(dto.getSeatType());
            t.setCarriageNo(item.getCarriageNo());
            t.setSeatNo(item.getSeatNo());
            t.setPassengerId(item.getPassengerId());
            t.setPassengerName(item.getPassengerName());
            t.setIdCardNo(item.getIdCardNo());
            t.setPrice(dto.getPrice());
            t.setStatus(0);   // 待支付
            t.setCreateTime(now);
            t.setUpdateTime(now);

            ticketMapper.insert(t);
            ticketNos.add(t.getTicketNo());
        }

        // 4. 存预占数据到 Redis（cancel 时归还座位用）
        SeatReserveData data = new SeatReserveData();
        data.setTrainNo(dto.getTrainNo());
        data.setRunDate(dto.getRunDate().toString());
        data.setSeatType(dto.getSeatType());
        data.setPrice(dto.getPrice());
        data.setPassengers(reserves);
        try {
            String json = objectMapper.writeValueAsString(data);
            stringRedisTemplate.opsForValue().set(RedisKeyConstant.seatReserveKey(dto.getOrderNo()), json, RESERVE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("预占数据存 Redis 失败（不影响主流程，cancel 时从 MySQL 读取）orderNo={}", dto.getOrderNo(), e);
        }

        // 清除该订单的票缓存
        evictTicketCache(dto.getOrderNo(), ticketNos);
        log.info("preAllocate 完成 orderNo={} count={} ticketNos={} seats={}",
                dto.getOrderNo(), reserves.size(), ticketNos,
                reserves.stream().map(SeatReserveItem::getSeatNo).toList());
        return new IssueVO(dto.getOrderNo(), reserves.size(), ticketNos);
    }

    // ==================== 支付成功：更新 ticket status 0→1 ====================

    @Override
    public IssueVO issueByOrderNo(IssueByOrderNoDTO dto) {
        // 1. 状态机：ticket status 0→1
        int affected = ticketMapper.confirmByOrderNo(dto.getOrderNo());
        if (affected == 0) {
            // 可能已确认过（幂等），或不存在 ticket 记录
            log.info("issueByOrderNo 幂等或无记录 orderNo={} affected={}", dto.getOrderNo(), affected);
        }

        // 2. 查询 ticket 列表返回
        List<TicketDO> tickets = ticketMapper.listByOrderNo(dto.getOrderNo());
        List<String> ticketNos = tickets.stream().map(TicketDO::getTicketNo).toList();

        // 3. 清除 Redis 预占数据（已不需要）
        stringRedisTemplate.delete(RedisKeyConstant.seatReserveKey(dto.getOrderNo()));

        // 4. 清除票缓存
        evictTicketCache(dto.getOrderNo(), ticketNos);
        log.info("issueByOrderNo 完成 orderNo={} count={} affected={}", dto.getOrderNo(), ticketNos.size(), affected);
        return new IssueVO(dto.getOrderNo(), ticketNos.size(), ticketNos);
    }

    // ==================== 取消/超时：归还座位 + 更新 ticket 状态 ====================

    @Override
    public int cancel(CancelDTO dto) {
        // 1. 查询 ticket 列表（从 MySQL 获取座位信息用于归还）
        List<TicketDO> tickets = ticketMapper.listByOrderNo(dto.getOrderNo());
        if (tickets == null || tickets.isEmpty()) {
            log.info("cancel 无 ticket 记录 orderNo={}", dto.getOrderNo());
            return 0;
        }

        // 2. 归还 Redis 座位池
        TicketDO first = tickets.get(0);
        for (TicketDO t : tickets) {
            if (t.getSeatNo() != null && t.getStatus() != null && t.getStatus() < 3) {
                seatPoolManager.push(first.getTrainNo(), first.getRunDate(),
                        first.getSeatType(), t.getSeatNo());
            }
        }

        // 3. 更新 ticket 状态 0/1 → 3
        int affected = ticketMapper.cancelByOrderNo(dto.getOrderNo());

        // 4. 清除 Redis 预占数据
        stringRedisTemplate.delete(RedisKeyConstant.seatReserveKey(dto.getOrderNo()));

        // 5. 清除票缓存
        evictTicketCache(dto.getOrderNo(),
                tickets.stream().map(TicketDO::getTicketNo).toList());

        log.info("cancel 完成 orderNo={} affected={} returnedSeats={}",
                dto.getOrderNo(), affected, tickets.size());
        return affected;
    }

    // ==================== 查询（Redis 缓存优先） ====================

    /** 缓存 TTL：10 分钟 */
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Override
    public List<TicketVO> listByOrderNo(String orderNo) {
        // 1. 查 Redis 缓存
        String cacheKey = RedisKeyConstant.ticketByOrderKey(orderNo);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<TicketVO>>() {});
            } catch (JsonProcessingException e) {
                log.warn("缓存解析失败，回源查询 orderNo={}", orderNo, e);
            }
        }

        // 2. 回源 MySQL
        List<TicketVO> result = ticketMapper.listByOrderNo(orderNo).stream()
                .map(this::toVO)
                .toList();

        // 3. 写入 Redis 缓存
        if (!result.isEmpty()) {
            try {
                stringRedisTemplate.opsForValue().set(cacheKey,
                        objectMapper.writeValueAsString(result), CACHE_TTL);
            } catch (JsonProcessingException e) {
                log.warn("缓存写入失败 orderNo={}", orderNo, e);
            }
        }
        return result;
    }

    @Override
    public TicketVO getByTicketNo(String ticketNo) {
        // 1. 查 Redis 缓存
        String cacheKey = RedisKeyConstant.ticketByTicketNoKey(ticketNo);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, TicketVO.class);
            } catch (JsonProcessingException e) {
                log.warn("缓存解析失败，回源查询 ticketNo={}", ticketNo, e);
            }
        }

        // 2. 回源 MySQL
        TicketDO t = ticketMapper.selectByTicketNo(ticketNo);
        if (t == null) {
            throw new BizException(ErrorCode.TICKET_NOT_FOUND);
        }
        TicketVO vo = toVO(t);

        // 3. 写入 Redis 缓存
        try {
            stringRedisTemplate.opsForValue().set(cacheKey,
                    objectMapper.writeValueAsString(vo), CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("缓存写入失败 ticketNo={}", ticketNo, e);
        }
        return vo;
    }

    private TicketVO toVO(TicketDO t) {
        TicketVO v = new TicketVO();
        v.setId(t.getId());
        v.setTicketNo(t.getTicketNo());
        v.setOrderNo(t.getOrderNo());
        v.setTrainNo(t.getTrainNo());
        v.setRunDate(t.getRunDate());
        v.setSeatType(t.getSeatType());
        v.setCarriageNo(t.getCarriageNo());
        v.setSeatNo(t.getSeatNo());
        v.setPassengerId(t.getPassengerId());
        v.setPassengerName(t.getPassengerName());
        v.setIdCardNo(IdCardValidator.mask(t.getIdCardNo()));
        v.setPrice(t.getPrice());
        v.setStatus(t.getStatus());
        v.setCreateTime(t.getCreateTime());
        return v;
    }

    /** 清除票相关缓存 */
    private void evictTicketCache(String orderNo, List<String> ticketNos) {
        try {
            stringRedisTemplate.delete(RedisKeyConstant.ticketByOrderKey(orderNo));
            for (String ticketNo : ticketNos) {
                stringRedisTemplate.delete(RedisKeyConstant.ticketByTicketNoKey(ticketNo));
            }
        } catch (Exception e) {
            log.warn("清除票缓存失败 orderNo={}", orderNo, e);
        }
    }

    // ==================== 内部 DTO ====================

    @lombok.Data
    public static class SeatReserveData {
        private String trainNo;
        private String runDate;
        private String seatType;
        private java.math.BigDecimal price;
        private List<SeatReserveItem> passengers;
    }

    @lombok.Data
    public static class SeatReserveItem {
        private Long passengerId;
        private String passengerName;
        private String idCardNo;
        private String seatNo;
        private int carriageNo;
    }
}
