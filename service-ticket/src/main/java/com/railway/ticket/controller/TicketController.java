package com.railway.ticket.controller;

import com.railway.common.annotation.AuthIgnore;
import com.railway.common.model.R;
import com.railway.ticket.dto.request.CancelDTO;
import com.railway.ticket.dto.request.IssueByOrderNoDTO;
import com.railway.ticket.dto.request.IssueDTO;
import com.railway.ticket.service.TicketService;
import com.railway.ticket.vo.IssueVO;
import com.railway.ticket.vo.TicketVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 票务接口。
 *
 * <p>网关路径：{@code /api/ticket/tickets/**}。
 *
 * <p>{@code /preAllocate | /issue | /cancel} 为服务间内部接口（{@code @AuthIgnore}），
 * 仅 service-order 通过 Feign 调用；用户查询走 {@code GET} 系列，需登录。
 */
@Slf4j
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /** 下单时预占座位（Redis SPOP + 写 MySQL ticket 表 status=0） */
    @AuthIgnore
    @PostMapping("/preAllocate")
    public R<IssueVO> preAllocate(@Valid @RequestBody IssueDTO dto) {
        return R.ok(ticketService.preAllocate(dto));
    }

    /** 支付成功后出票（更新 ticket status 0→1） */
    @AuthIgnore
    @PostMapping("/issue")
    public R<IssueVO> issue(@Valid @RequestBody IssueByOrderNoDTO dto) {
        return R.ok(ticketService.issueByOrderNo(dto));
    }

    /** 取消/超时：归还座位 */
    @AuthIgnore
    @PostMapping("/cancel")
    public R<Integer> cancel(@Valid @RequestBody CancelDTO dto) {
        return R.ok(ticketService.cancel(dto));
    }

    /** 用户查自己订单的票（需登录） */
    @GetMapping("/order/{orderNo}")
    public R<List<TicketVO>> listByOrderNo(@PathVariable String orderNo) {
        return R.ok(ticketService.listByOrderNo(orderNo));
    }

    @GetMapping("/{ticketNo}")
    public R<TicketVO> getByTicketNo(@PathVariable String ticketNo) {
        return R.ok(ticketService.getByTicketNo(ticketNo));
    }
}
