package com.railway.order.manager;

import com.railway.common.model.R;
import com.railway.order.feign.TicketFeignClient;
import com.railway.order.feign.dto.CancelTicketDTO;
import com.railway.order.feign.dto.IssueByOrderNoDTO;
import com.railway.order.feign.dto.IssueTicketDTO;
import com.railway.order.feign.dto.PassengerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 票务编排：封装 Feign 调用 ticket。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTicketManager {

    private final TicketFeignClient ticketFeignClient;

    /** 下单时预占座位（Redis SPOP + 写 MySQL ticket 表 status=0） */
    public void preAllocate(String orderNo, String trainNo, LocalDate runDate, String seatType,
                            BigDecimal price, List<PassengerVO> passengers) {
        IssueTicketDTO dto = buildIssueDTO(orderNo, trainNo, runDate, seatType, price, passengers);
        R<?> r = ticketFeignClient.preAllocate(dto);
        if (r == null || r.getCode() != 200) {
            throw new com.railway.common.exception.BizException(
                    com.railway.common.exception.ErrorCode.TICKET_ISSUE_FAILED,
                    r == null ? "ticket.preAllocate 失败（无响应）" : r.getMessage());
        }
    }

    /** 支付成功后出票（更新 ticket status 0→1） */
    public void issue(String orderNo, LocalDate runDate) {
        IssueByOrderNoDTO dto = new IssueByOrderNoDTO();
        dto.setOrderNo(orderNo);
        dto.setRunDate(runDate);
        R<?> r = ticketFeignClient.issue(dto);
        if (r == null || r.getCode() != 200) {
            throw new com.railway.common.exception.BizException(
                    com.railway.common.exception.ErrorCode.TICKET_ISSUE_FAILED,
                    r == null ? "ticket.issue 失败（无响应）" : r.getMessage());
        }
    }

    public void cancel(String orderNo) {
        CancelTicketDTO dto = new CancelTicketDTO();
        dto.setOrderNo(orderNo);
        R<?> r = ticketFeignClient.cancel(dto);
        if (r == null || r.getCode() != 200) {
            log.error("ticket.cancel 失败 orderNo={} msg={}", orderNo, r == null ? "null" : r.getMessage());
        }
    }

    private IssueTicketDTO buildIssueDTO(String orderNo, String trainNo, LocalDate runDate,
                                         String seatType, BigDecimal price, List<PassengerVO> passengers) {
        IssueTicketDTO dto = new IssueTicketDTO();
        dto.setOrderNo(orderNo);
        dto.setTrainNo(trainNo);
        dto.setRunDate(runDate);
        dto.setSeatType(seatType);
        dto.setPrice(price);
        dto.setPassengers(passengers.stream()
                .map(p -> {
                    IssueTicketDTO.PassengerItem item = new IssueTicketDTO.PassengerItem();
                    item.setPassengerId(p.getId());
                    item.setPassengerName(p.getName());
                    item.setIdCardNo(p.getIdCardNo());
                    return item;
                })
                .toList());
        return dto;
    }
}
