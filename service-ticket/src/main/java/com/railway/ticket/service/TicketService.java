package com.railway.ticket.service;

import com.railway.ticket.dto.request.CancelDTO;
import com.railway.ticket.dto.request.ConfirmDTO;
import com.railway.ticket.dto.request.IssueDTO;
import com.railway.ticket.vo.IssueVO;
import com.railway.ticket.vo.TicketVO;

import java.util.List;

/**
 * 票务服务：出票 / 确认 / 退票。
 * <p>被 service-order 通过 Feign 调用（{@code /tickets/issue|confirm|cancel}）；
 * 也对外暴露查询接口。
 */
public interface TicketService {

    /**
     * 出票。幂等：同一 {@code orderNo} 多次调用返回首次结果。
     */
    IssueVO issue(IssueDTO dto);

    /**
     * 确认出票（支付成功回调）。order 状态 0 → 1。
     */
    int confirm(ConfirmDTO dto);

    /**
     * 退票。order 状态 0/1 → 3，同时归还座位到池。
     */
    int cancel(CancelDTO dto);

    /**
     * 按订单号查所有票（已脱敏）。
     */
    List<TicketVO> listByOrderNo(String orderNo);

    /**
     * 按 ticketNo 查单张票（已脱敏）。
     */
    TicketVO getByTicketNo(String ticketNo);
}
