package com.railway.order.manager;

import com.railway.common.model.R;
import com.railway.order.feign.TicketFeignClient;
import com.railway.order.feign.dto.CancelTicketDTO;
import com.railway.order.feign.dto.ConfirmTicketDTO;
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

    public void issue(String orderNo, String trainNo, LocalDate runDate, String seatType,
                      BigDecimal price, List<PassengerVO> passengers) {
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
        R<Void> r = ticketFeignClient.issue(dto);
        if (r == null || r.getCode() != 200) {
            throw new com.railway.common.exception.BizException(
                    com.railway.common.exception.ErrorCode.TICKET_ISSUE_FAILED,
                    r == null ? "ticket.issue 失败（无响应）" : r.getMessage());
        }
    }

    public void confirm(String orderNo) {
        ConfirmTicketDTO dto = new ConfirmTicketDTO();
        dto.setOrderNo(orderNo);
        R<Void> r = ticketFeignClient.confirm(dto);
        if (r == null || r.getCode() != 200) {
            log.error("ticket.confirm 失败 orderNo={} msg={}", orderNo, r == null ? "null" : r.getMessage());
        }
    }

    public void cancel(String orderNo) {
        CancelTicketDTO dto = new CancelTicketDTO();
        dto.setOrderNo(orderNo);
        R<Void> r = ticketFeignClient.cancel(dto);
        if (r == null || r.getCode() != 200) {
            log.error("ticket.cancel 失败 orderNo={} msg={}", orderNo, r == null ? "null" : r.getMessage());
        }
    }
}
