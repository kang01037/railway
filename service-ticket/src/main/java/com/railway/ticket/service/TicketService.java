package com.railway.ticket.service;

import com.railway.ticket.dto.request.CancelDTO;
import com.railway.ticket.dto.request.IssueByOrderNoDTO;
import com.railway.ticket.dto.request.IssueDTO;
import com.railway.ticket.vo.IssueVO;
import com.railway.ticket.vo.TicketVO;

import java.util.List;

/**
 * 票务服务：预占座 / 出票 / 退票。
 * <p>被 service-order 通过 Feign 调用；也对外暴露查询接口。
 *
 * <p>流程：
 * <ol>
 *   <li>下单时：{@link #preAllocate} — Redis SPOP 预占座位 + 写 MySQL ticket 表（status=0）</li>
 *   <li>支付成功：{@link #issueByOrderNo} — 更新 ticket status 0→1（已出票）</li>
 *   <li>取消/超时：{@link #cancel} — 归还 Redis 座位 + 更新 ticket 状态</li>
 * </ol>
 */
public interface TicketService {

    /**
     * 预占座位（下单时调用）。Redis SPOP + 写 MySQL ticket 表（status=0 待支付）。
     * 幂等：同一 orderNo 多次调用返回首次结果。
     */
    IssueVO preAllocate(IssueDTO dto);

    /**
     * 出票（支付成功后调用）。更新 ticket status 0→1，不再依赖 Redis 预占数据。
     * 幂等：同一 orderNo 已出票 → 直接返回。
     */
    IssueVO issueByOrderNo(IssueByOrderNoDTO dto);

    /**
     * 退票（取消/超时）。归还座位到池，清理 Redis 预占数据。
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
