package com.railway.order.service.impl;

import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.util.SnowflakeIdWorker;
import com.railway.order.dto.request.CreateOrderDTO;
import com.railway.order.entity.OrderDO;
import com.railway.order.feign.PaymentFeignClient;
import com.railway.order.feign.StockFeignClient;
import com.railway.order.feign.TicketFeignClient;
import com.railway.order.feign.UserFeignClient;
import com.railway.order.feign.dto.PassengerVO;
import com.railway.order.feign.dto.PayVO;
import com.railway.order.feign.dto.TicketVO;
import com.railway.order.manager.OrderPayManager;
import com.railway.order.manager.OrderStockManager;
import com.railway.order.manager.OrderTicketManager;
import com.railway.order.manager.OrderUserManager;
import com.railway.order.mapper.OrderMapper;
import com.railway.order.vo.CreateOrderResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderServiceImpl 单测：create 完整编排（happy + 失败路径）。
 * <p>所有 Feign / Manager / Mapper / RabbitTemplate 全部 mock。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderStockManager stockManager;
    @Mock private OrderTicketManager ticketManager;
    @Mock private OrderPayManager payManager;
    @Mock private OrderUserManager userManager;
    @Mock private SnowflakeIdWorker snowflakeIdWorker;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private OrderServiceImpl orderService;

    private CreateOrderDTO sampleDto() {
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setTrainNo("G1234");
        dto.setRunDate(LocalDate.of(2026, 6, 10));
        dto.setSeatType("SECOND");
        dto.setPrice(new BigDecimal("1058.00"));
        dto.setPassengerIds(List.of(1001L, 1002L));
        return dto;
    }

    // ============== create ==============

    @Test
    void create_happyPath_returnsPayUrl() {
        CreateOrderDTO dto = sampleDto();
        // 1. userManager 拿乘客
        PassengerVO p1 = new PassengerVO();
        p1.setId(1001L);
        p1.setName("张三");
        p1.setIdCardNo("11010519491231002X");
        PassengerVO p2 = new PassengerVO();
        p2.setId(1002L);
        p2.setName("李四");
        p2.setIdCardNo("11010519491231002Y");
        when(userManager.listByIds(dto.getPassengerIds())).thenReturn(Map.of(1001L, p1, 1002L, p2));

        // 2. 雪花 orderNo
        when(snowflakeIdWorker.nextId()).thenReturn(12345L, 67890L);

        // 3. stock.occupy 不抛

        // 4. ticket.issue 不抛
        // 5. pay 返回 payUrl
        PayVO pay = new PayVO();
        pay.setPayNo("Pxxx");
        pay.setPayUrl("http://localhost:9000/api/payment/callback/sim?payNo=Pxxx");
        when(payManager.createPay(anyString(), any(BigDecimal.class))).thenReturn(pay);

        CreateOrderResultVO vo = orderService.create(dto, 99L);

        assertNotNull(vo);
        assertNotNull(vo.getOrderNo());
        assertTrue(vo.getOrderNo().startsWith("O"));
        assertEquals(0, vo.getAmount().compareTo(new BigDecimal("2116.00")));   // 1058 * 2
        assertEquals(pay.getPayUrl(), vo.getPayUrl());
        assertNotNull(vo.getExpireTime());

        // 验证调用链顺序
        verify(stockManager, times(1)).occupy(eq(vo.getOrderNo()), eq("G1234"),
                eq(LocalDate.of(2026, 6, 10)), eq("SECOND"), eq(2));
        verify(orderMapper, times(1)).insert(any(OrderDO.class));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class), any(org.springframework.amqp.core.MessagePostProcessor.class));
        verify(ticketManager, times(1)).issue(eq(vo.getOrderNo()), anyString(), any(), anyString(),
                any(BigDecimal.class), any());
        verify(payManager, times(1)).createPay(anyString(), any(BigDecimal.class));
    }

    @Test
    void create_userIdNull_throwsUnauthorized() {
        BizException ex = assertThrows(BizException.class,
                () -> orderService.create(sampleDto(), null));
        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void create_tooManyPassengers_throwsBadRequest() {
        CreateOrderDTO dto = sampleDto();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 6; i++) ids.add(1000L + i);
        dto.setPassengerIds(ids);

        BizException ex = assertThrows(BizException.class,
                () -> orderService.create(dto, 99L));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void create_stockOccupyFails_throwsNotEnough() {
        CreateOrderDTO dto = sampleDto();
        when(userManager.listByIds(any())).thenReturn(Map.of());
        when(snowflakeIdWorker.nextId()).thenReturn(1L, 2L);

        org.mockito.Mockito.doThrow(new BizException(ErrorCode.STOCK_NOT_ENOUGH))
                .when(stockManager).occupy(anyString(), anyString(), any(), anyString(), anyInt());

        BizException ex = assertThrows(BizException.class,
                () -> orderService.create(dto, 99L));
        assertEquals(ErrorCode.STOCK_NOT_ENOUGH.getCode(), ex.getCode());
        verify(orderMapper, never()).insert(any());
        verify(ticketManager, never()).issue(anyString(), anyString(), any(), anyString(), any(), any());
    }

    @Test
    void create_ticketIssueFails_compensatesStockAndOrder() {
        CreateOrderDTO dto = sampleDto();
        when(userManager.listByIds(any())).thenReturn(Map.of());
        when(snowflakeIdWorker.nextId()).thenReturn(1L, 2L);

        org.mockito.Mockito.doThrow(new BizException(ErrorCode.TICKET_ISSUE_FAILED, "出票失败"))
                .when(ticketManager).issue(anyString(), anyString(), any(), anyString(),
                        any(BigDecimal.class), any());

        BizException ex = assertThrows(BizException.class,
                () -> orderService.create(dto, 99L));
        assertEquals(ErrorCode.TICKET_ISSUE_FAILED.getCode(), ex.getCode());

        // 补偿：释放库存 + 改状态 3
        verify(stockManager, times(1)).release(anyString(), eq("G1234"),
                eq(LocalDate.of(2026, 6, 10)), eq("SECOND"), eq(2));
        verify(orderMapper, times(1)).refundByOrderNo(anyString());
    }
}
