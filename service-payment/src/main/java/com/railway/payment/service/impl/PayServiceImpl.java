package com.railway.payment.service.impl;

import com.railway.common.constant.MqConstant;
import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.util.SnowflakeIdWorker;
import com.railway.payment.config.PayProperties;
import com.railway.payment.dto.request.CallbackDTO;
import com.railway.payment.dto.request.CreatePayDTO;
import com.railway.payment.entity.PayRecordDO;
import com.railway.payment.feign.OrderFeignClient;
import com.railway.payment.mapper.PayRecordMapper;
import com.railway.payment.service.PayService;
import com.railway.payment.vo.PayVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付服务实现。
 *
 * <h3>createPay</h3>
 * <ol>
 *   <li>雪花 ID 生成 {@code payNo = P + snowflakeId}。</li>
 *   <li>写 pay_record status=0（待支付）。</li>
 *   <li>返回 payUrl = {@code callbackBaseUrl}/sim?payNo=xxx。</li>
 * </ol>
 *
 * <h3>callback</h3>
 * <ol>
 *   <li>UPDATE {@code pay_record SET status=? WHERE pay_no=? AND status=0}。affected=0 → 已处理过，幂等命中。</li>
 *   <li>affected=1 → 发 RabbitMQ {@code order.exchange / order.paid}，body={orderNo, payNo, success}。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private final PayRecordMapper payRecordMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final RabbitTemplate rabbitTemplate;
    private final PayProperties payProperties;
    private final OrderFeignClient orderFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayVO createPay(CreatePayDTO dto) {
        // 1. 雪花 payNo
        String payNo = "P" + snowflakeIdWorker.nextId();

        // 2. 写 pay_record
        PayRecordDO record = new PayRecordDO();
        record.setId(snowflakeIdWorker.nextId());
        record.setPayNo(payNo);
        record.setOrderNo(dto.getOrderNo());
        record.setAmount(dto.getAmount());
        record.setPayChannel(dto.getPayChannel());
        record.setStatus(0);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        payRecordMapper.insert(record);

        // 3. 拼 payUrl
        String payUrl = payProperties.getCallbackBaseUrl() + "/sim?payNo=" + payNo;

        log.info("createPay payNo={} orderNo={} amount={} channel={} payUrl={}",
                payNo, dto.getOrderNo(), dto.getAmount(), dto.getPayChannel(), payUrl);
        return toVO(record, payUrl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean callback(CallbackDTO dto) {
        // 1. 状态机更新（0 → 1 成功 / 2 失败）
        int newStatus = Boolean.TRUE.equals(dto.getSuccess()) ? 1 : 2;
        int affected = payRecordMapper.updateStatusFromPending(dto.getPayNo(), newStatus);
        if (affected == 0) {
            log.info("callback 幂等命中（已处理）payNo={} success={}", dto.getPayNo(), dto.getSuccess());
            return false;
        }

        // 2. 查 orderNo 用于发 MQ
        PayRecordDO record = payRecordMapper.selectByPayNo(dto.getPayNo());
        if (record == null) {
            // 极端情况：affected=1 但 selectByPayNo 为空？基本不可能
            throw new BizException(ErrorCode.PAY_NOT_FOUND);
        }

        // 3. 异步化：直接发 MQ，不再同步调用 order 服务
        Map<String, Object> msg = new HashMap<>(4);
        msg.put("orderNo", record.getOrderNo());
        msg.put("payNo", record.getPayNo());
        msg.put("amount", record.getAmount());
        msg.put("success", dto.getSuccess());

        rabbitTemplate.convertAndSend(MqConstant.ORDER_EXCHANGE, MqConstant.RK_ORDER_PAID, msg);
        log.info("callback 异步化：发送 MQ order.paid orderNo={} success={}", record.getOrderNo(), dto.getSuccess());

        return true;
    }

    @Override
    public PayVO getByPayNo(String payNo) {
        PayRecordDO r = payRecordMapper.selectByPayNo(payNo);
        if (r == null) {
            throw new BizException(ErrorCode.PAY_NOT_FOUND);
        }
        return toVO(r, payProperties.getCallbackBaseUrl() + "/sim?payNo=" + payNo);
    }

    @Override
    public List<PayVO> listByOrderNo(String orderNo) {
        return payRecordMapper.listByOrderNo(orderNo).stream()
                .map(r -> toVO(r, payProperties.getCallbackBaseUrl() + "/sim?payNo=" + r.getPayNo()))
                .toList();
    }

    private PayVO toVO(PayRecordDO r, String payUrl) {
        PayVO v = new PayVO();
        v.setPayNo(r.getPayNo());
        v.setOrderNo(r.getOrderNo());
        v.setAmount(r.getAmount());
        v.setPayChannel(r.getPayChannel());
        v.setPayUrl(payUrl);
        v.setStatus(r.getStatus());
        v.setPaidTime(r.getPaidTime());
        v.setCreateTime(r.getCreateTime());
        return v;
    }
}
