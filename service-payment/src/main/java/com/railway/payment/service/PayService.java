package com.railway.payment.service;

import com.railway.payment.dto.request.CallbackDTO;
import com.railway.payment.dto.request.CreatePayDTO;
import com.railway.payment.vo.PayVO;

import java.util.List;

/**
 * 支付服务：发起 / 回调 / 查询。
 */
public interface PayService {

    /**
     * 发起支付：写 pay_record(status=0)，返回 payUrl。
     */
    PayVO createPay(CreatePayDTO dto);

    /**
     * 支付回调（模拟）：update pay_record + 发 MQ {@code order.paid}。
     * <p>幂等：仅在 status=0 时改状态 + 发消息；已处理过则 noop 返回 false。
     *
     * @return true 本次处理，false 已处理过（幂等命中）
     */
    boolean callback(CallbackDTO dto);

    /** 按 payNo 查（VO） */
    PayVO getByPayNo(String payNo);

    /** 按 orderNo 查所有支付单（VO） */
    List<PayVO> listByOrderNo(String orderNo);
}
