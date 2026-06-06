package com.railway.ticket.service.impl;

import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.util.IdCardValidator;
import com.railway.common.util.SnowflakeIdWorker;
import com.railway.ticket.dto.request.CancelDTO;
import com.railway.ticket.dto.request.ConfirmDTO;
import com.railway.ticket.dto.request.IssueDTO;
import com.railway.ticket.entity.TicketDO;
import com.railway.ticket.mapper.TicketMapper;
import com.railway.ticket.manager.SeatPoolManager;
import com.railway.ticket.service.TicketService;
import com.railway.ticket.vo.IssueVO;
import com.railway.ticket.vo.TicketVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketMapper ticketMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final SeatPoolManager seatPoolManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IssueVO issue(IssueDTO dto) {
        // 0. 幂等：同一 orderNo 已出票 → 直接返回
        if (ticketMapper.countByOrderNo(dto.getOrderNo()) > 0) {
            List<TicketDO> exist = ticketMapper.listByOrderNo(dto.getOrderNo());
            log.info("issue 幂等 orderNo={} 已存在 ticket 数={}", dto.getOrderNo(), exist.size());
            return new IssueVO(dto.getOrderNo(), exist.size(),
                    exist.stream().map(TicketDO::getTicketNo).toList());
        }

        // 1. 初始化座位池（按 DB total 懒加载；简化：total = 人数 * 8，最小 20）
        int poolSize = Math.max(20, dto.getPassengers().size() * 8);
        seatPoolManager.initIfAbsent(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType(), poolSize);

        // 2. 循环出票
        List<String> ticketNos = new ArrayList<>(dto.getPassengers().size());
        LocalDateTime now = LocalDateTime.now();
        for (IssueDTO.PassengerItem p : dto.getPassengers()) {
            // 身份证合法性（防御）
            if (!IdCardValidator.isValid(p.getIdCardNo())) {
                throw new BizException(ErrorCode.BAD_REQUEST,
                        "乘车人证件号不合法 passengerId=" + p.getPassengerId());
            }

            TicketDO t = new TicketDO();
            t.setId(snowflakeIdWorker.nextId());
            t.setTicketNo("T" + t.getId());
            t.setOrderNo(dto.getOrderNo());
            t.setTrainNo(dto.getTrainNo());
            t.setRunDate(dto.getRunDate());
            t.setSeatType(dto.getSeatType());

            // 选座
            String seatNo = seatPoolManager.pop(dto.getTrainNo(), dto.getRunDate(), dto.getSeatType());
            if (seatNo == null) {
                // 池空：兜底（不应发生，stock-service 已守门）
                log.warn("座位池空，兜底使用 -1 orderNo={} passengerId={}", dto.getOrderNo(), p.getPassengerId());
                t.setCarriageNo(0);
                t.setSeatNo("POOL_EMPTY");
            } else {
                t.setCarriageNo(seatPoolManager.defaultCarriage());
                t.setSeatNo(seatNo);
            }

            t.setPassengerId(p.getPassengerId());
            t.setPassengerName(p.getPassengerName());
            t.setIdCardNo(p.getIdCardNo());
            t.setPrice(dto.getPrice());
            t.setStatus(0);   // 待支付
            t.setCreateTime(now);
            t.setUpdateTime(now);

            ticketMapper.insert(t);
            ticketNos.add(t.getTicketNo());
        }
        log.info("issue 完成 orderNo={} count={} ticketNos={}", dto.getOrderNo(), ticketNos.size(), ticketNos);
        return new IssueVO(dto.getOrderNo(), ticketNos.size(), ticketNos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirm(ConfirmDTO dto) {
        int affected = ticketMapper.confirmByOrderNo(dto.getOrderNo());
        log.info("confirm orderNo={} affected={}", dto.getOrderNo(), affected);
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancel(CancelDTO dto) {
        // 1. 状态机：0/1 → 3
        int affected = ticketMapper.cancelByOrderNo(dto.getOrderNo());
        if (affected == 0) {
            log.info("cancel 无可改票 orderNo={}", dto.getOrderNo());
            return 0;
        }

        // 2. 退座位到池（仅对 status=0 的票 — 因为 status=1 的票已真正"卖出"，通常不退；这里按 dev 文档简化都还）
        List<TicketDO> tickets = ticketMapper.listByOrderNo(dto.getOrderNo());
        if (!CollectionUtils.isEmpty(tickets) && !tickets.isEmpty()) {
            // 取第一条拿到上下文（同一 order 的 runDate / trainNo / seatType 都一样）
            TicketDO first = tickets.get(0);
            for (TicketDO t : tickets) {
                if (t.getStatus() != null && t.getStatus() == 3 && t.getSeatNo() != null) {
                    seatPoolManager.push(first.getTrainNo(), first.getRunDate(),
                            first.getSeatType(), t.getSeatNo());
                }
            }
        }
        log.info("cancel orderNo={} affected={} seatsReturned={}",
                dto.getOrderNo(), affected,
                tickets == null ? 0 : tickets.size());
        return affected;
    }

    @Override
    public List<TicketVO> listByOrderNo(String orderNo) {
        return ticketMapper.listByOrderNo(orderNo).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public TicketVO getByTicketNo(String ticketNo) {
        TicketDO t = ticketMapper.selectByTicketNo(ticketNo);
        if (t == null) {
            throw new BizException(ErrorCode.TICKET_NOT_FOUND);
        }
        return toVO(t);
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
}
