package com.railway.payment.service.impl;

import com.railway.common.constant.MqConstant;
import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.util.SnowflakeIdWorker;
import com.railway.payment.config.PayProperties;
import com.railway.payment.dto.request.CallbackDTO;
import com.railway.payment.dto.request.CreatePayDTO;
import com.railway.payment.entity.PayRecordDO;
import com.railway.payment.mapper.PayRecordMapper;
import com.railway.payment.vo.PayVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PayServiceImpl 单测：createPay / callback（幂等 + MQ）。
 */
@ExtendWith(MockitoExtension.class)
class PayServiceImplTest {

    @Mock private PayRecordMapper payRecordMapper;
    @Mock private SnowflakeIdWorker snowflakeIdWorker;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private PayProperties payProperties;

    @InjectMocks private PayServiceImpl payService;

    // ============== createPay ==============

    @Test
    void createPay_insertsRecordAndReturnsUrl() {
        CreatePayDTO dto = new CreatePayDTO();
        dto.setOrderNo("O12345");
        dto.setAmount(new BigDecimal("1058.00"));
        dto.setPayChannel("SIM");

        when(snowflakeIdWorker.nextId()).thenReturn(100L, 200L);
        when(payProperties.getCallbackBaseUrl()).thenReturn("http://localhost:9000/api/payment/callback");

        PayVO vo = payService.createPay(dto);

        assertNotNull(vo);
        assertEquals("O12345", vo.getOrderNo());
        assertTrue(vo.getPayNo().startsWith("P"));
        assertTrue(vo.getPayUrl().startsWith("http://localhost:9000/api/payment/callback/sim?payNo=P"));
        assertTrue(vo.getPayUrl().contains("P"));
        verify(payRecordMapper, times(1)).insert(any(PayRecordDO.class));
    }

    // ============== callback ==============

    @Test
    void callback_success_updatesAndSendsMq() {
        CallbackDTO dto = new CallbackDTO();
        dto.setPayNo("P123");
        dto.setSuccess(true);

        when(payRecordMapper.updateStatusFromPending("P123", 1)).thenReturn(1);
        PayRecordDO rec = new PayRecordDO();
        rec.setPayNo("P123");
        rec.setOrderNo("O999");
        rec.setAmount(new BigDecimal("500.00"));
        rec.setPayChannel("SIM");
        rec.setStatus(1);
        when(payRecordMapper.selectByPayNo("P123")).thenReturn(rec);

        boolean changed = payService.callback(dto);
        assertTrue(changed);

        ArgumentCaptor<java.util.Map<String, Object>> msgCap = ArgumentCaptor.forClass(java.util.Map.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(MqConstant.ORDER_EXCHANGE), eq(MqConstant.RK_ORDER_PAID), msgCap.capture());
        assertEquals("O999", msgCap.getValue().get("orderNo"));
        assertEquals("P123", msgCap.getValue().get("payNo"));
        assertEquals(true, msgCap.getValue().get("success"));
    }

    @Test
    void callback_idempotent_noOpAndNoMq() {
        CallbackDTO dto = new CallbackDTO();
        dto.setPayNo("P123");
        dto.setSuccess(true);

        when(payRecordMapper.updateStatusFromPending("P123", 1)).thenReturn(0);

        boolean changed = payService.callback(dto);
        assertFalse(changed);
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void callback_failure_setsStatus2AndStillSendsMq() {
        CallbackDTO dto = new CallbackDTO();
        dto.setPayNo("P456");
        dto.setSuccess(false);

        when(payRecordMapper.updateStatusFromPending("P456", 2)).thenReturn(1);
        PayRecordDO rec = new PayRecordDO();
        rec.setPayNo("P456");
        rec.setOrderNo("O777");
        rec.setAmount(new BigDecimal("200.00"));
        rec.setPayChannel("SIM");
        rec.setStatus(2);
        when(payRecordMapper.selectByPayNo("P456")).thenReturn(rec);

        boolean changed = payService.callback(dto);
        assertTrue(changed);
        verify(payRecordMapper, times(1)).updateStatusFromPending("P456", 2);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(MqConstant.ORDER_EXCHANGE), eq(MqConstant.RK_ORDER_PAID), any(Object.class));
    }

    // ============== getByPayNo ==============

    @Test
    void getByPayNo_notFound_throws() {
        when(payRecordMapper.selectByPayNo("P-NONE")).thenReturn(null);
        BizException ex = assertThrows(BizException.class,
                () -> payService.getByPayNo("P-NONE"));
        assertEquals(ErrorCode.PAY_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void getByPayNo_returnsVo() {
        PayRecordDO r = new PayRecordDO();
        r.setPayNo("P-OK");
        r.setOrderNo("O1");
        r.setAmount(new BigDecimal("100.00"));
        r.setPayChannel("SIM");
        r.setStatus(1);
        r.setPaidTime(LocalDateTime.now());
        when(payRecordMapper.selectByPayNo("P-OK")).thenReturn(r);
        when(payProperties.getCallbackBaseUrl()).thenReturn("http://x");

        PayVO vo = payService.getByPayNo("P-OK");
        assertEquals("P-OK", vo.getPayNo());
        assertEquals(1, vo.getStatus());
        assertTrue(vo.getPayUrl().contains("P-OK"));
    }
}
