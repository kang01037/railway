package com.railway.ticket.service.impl;

import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.util.SnowflakeIdWorker;
import com.railway.ticket.dto.request.CancelDTO;
import com.railway.ticket.dto.request.ConfirmDTO;
import com.railway.ticket.dto.request.IssueDTO;
import com.railway.ticket.entity.TicketDO;
import com.railway.ticket.mapper.TicketMapper;
import com.railway.ticket.manager.SeatPoolManager;
import com.railway.ticket.vo.IssueVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
 * TicketServiceImpl 单测：issue / confirm / cancel / 幂等。
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock private TicketMapper ticketMapper;
    @Mock private SnowflakeIdWorker snowflakeIdWorker;
    @Mock private SeatPoolManager seatPoolManager;

    @InjectMocks private TicketServiceImpl ticketService;

    private IssueDTO sampleDto(int passengerCount) {
        IssueDTO dto = new IssueDTO();
        dto.setOrderNo("O12345");
        dto.setTrainNo("G1234");
        dto.setRunDate(LocalDate.of(2026, 6, 10));
        dto.setSeatType("SECOND");
        dto.setPrice(new BigDecimal("1058.00"));
        List<IssueDTO.PassengerItem> list = new ArrayList<>();
        for (int i = 0; i < passengerCount; i++) {
            IssueDTO.PassengerItem p = new IssueDTO.PassengerItem();
            p.setPassengerId(1000L + i);
            p.setPassengerName("乘客" + i);
            p.setIdCardNo("11010519491231002X");     // 合法
            list.add(p);
        }
        dto.setPassengers(list);
        return dto;
    }

    // ============== issue ==============

    @Test
    void issue_happyPath_createsTicketsAndPopsSeats() {
        IssueDTO dto = sampleDto(2);
        when(ticketMapper.countByOrderNo("O12345")).thenReturn(0);
        when(snowflakeIdWorker.nextId()).thenReturn(1L, 2L);
        when(seatPoolManager.pop(anyString(), any(), anyString())).thenReturn("1", "2");
        when(seatPoolManager.defaultCarriage()).thenReturn(1);

        IssueVO vo = ticketService.issue(dto);

        assertEquals("O12345", vo.getOrderNo());
        assertEquals(2, vo.getTicketCount());
        assertEquals(2, vo.getTicketNos().size());
        assertTrue(vo.getTicketNos().get(0).startsWith("T"));
        verify(seatPoolManager, times(1)).initIfAbsent(eq("G1234"), any(), eq("SECOND"), anyInt());
        verify(ticketMapper, times(2)).insert(any(TicketDO.class));
    }

    @Test
    void issue_idempotent_returnsExistingTickets() {
        IssueDTO dto = sampleDto(2);
        when(ticketMapper.countByOrderNo("O12345")).thenReturn(2);

        TicketDO t1 = new TicketDO();
        t1.setTicketNo("T-EXIST-1");
        t1.setOrderNo("O12345");
        TicketDO t2 = new TicketDO();
        t2.setTicketNo("T-EXIST-2");
        t2.setOrderNo("O12345");
        when(ticketMapper.listByOrderNo("O12345")).thenReturn(List.of(t1, t2));

        IssueVO vo = ticketService.issue(dto);
        assertEquals(2, vo.getTicketCount());
        assertTrue(vo.getTicketNos().contains("T-EXIST-1"));
        verify(ticketMapper, never()).insert(any());
    }

    @Test
    void issue_invalidIdCard_throws() {
        IssueDTO dto = sampleDto(1);
        dto.getPassengers().get(0).setIdCardNo("NOT_A_VALID_IDCARD");
        when(ticketMapper.countByOrderNo(anyString())).thenReturn(0);

        BizException ex = assertThrows(BizException.class, () -> ticketService.issue(dto));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(ticketMapper, never()).insert(any());
    }

    @Test
    void issue_seatPoolEmpty_fallsBackToPlaceholder() {
        IssueDTO dto = sampleDto(1);
        when(ticketMapper.countByOrderNo("O12345")).thenReturn(0);
        when(snowflakeIdWorker.nextId()).thenReturn(99L);
        when(seatPoolManager.pop(anyString(), any(), anyString())).thenReturn(null);  // 池空

        IssueVO vo = ticketService.issue(dto);
        assertEquals(1, vo.getTicketCount());
        ArgumentCaptor<TicketDO> captor = ArgumentCaptor.forClass(TicketDO.class);
        verify(ticketMapper).insert(captor.capture());
        assertEquals("POOL_EMPTY", captor.getValue().getSeatNo());
    }

    // ============== confirm ==============

    @Test
    void confirm_returnsAffectedRows() {
        ConfirmDTO dto = new ConfirmDTO();
        dto.setOrderNo("O12345");
        when(ticketMapper.confirmByOrderNo("O12345")).thenReturn(2);

        int affected = ticketService.confirm(dto);
        assertEquals(2, affected);
    }

    @Test
    void confirm_noTicketsAffected_returnsZero() {
        ConfirmDTO dto = new ConfirmDTO();
        dto.setOrderNo("O_NOTEXIST");
        when(ticketMapper.confirmByOrderNo("O_NOTEXIST")).thenReturn(0);

        assertEquals(0, ticketService.confirm(dto));
    }

    // ============== cancel ==============

    @Test
    void cancel_returnsSeatsToPool() {
        CancelDTO dto = new CancelDTO();
        dto.setOrderNo("O12345");
        when(ticketMapper.cancelByOrderNo("O12345")).thenReturn(2);

        TicketDO t1 = new TicketDO();
        t1.setTrainNo("G1234");
        t1.setRunDate(LocalDate.of(2026, 6, 10));
        t1.setSeatType("SECOND");
        t1.setSeatNo("5");
        t1.setStatus(3);   // 已退
        TicketDO t2 = new TicketDO();
        t2.setTrainNo("G1234");
        t2.setRunDate(LocalDate.of(2026, 6, 10));
        t2.setSeatType("SECOND");
        t2.setSeatNo("6");
        t2.setStatus(3);
        when(ticketMapper.listByOrderNo("O12345")).thenReturn(List.of(t1, t2));

        int affected = ticketService.cancel(dto);
        assertEquals(2, affected);
        verify(seatPoolManager, times(2))
                .push(eq("G1234"), any(), eq("SECOND"), anyString());
    }

    @Test
    void cancel_alreadyCancelled_returnsZero() {
        CancelDTO dto = new CancelDTO();
        dto.setOrderNo("O12345");
        when(ticketMapper.cancelByOrderNo("O12345")).thenReturn(0);

        assertEquals(0, ticketService.cancel(dto));
        verify(seatPoolManager, never()).push(anyString(), any(), anyString(), anyString());
    }

    // ============== getByTicketNo ==============

    @Test
    void getByTicketNo_notFound_throws() {
        when(ticketMapper.selectByTicketNo("T-NONE")).thenReturn(null);
        BizException ex = assertThrows(BizException.class,
                () -> ticketService.getByTicketNo("T-NONE"));
        assertEquals(ErrorCode.TICKET_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void getByTicketNo_masksIdCard() {
        TicketDO t = new TicketDO();
        t.setId(1L);
        t.setTicketNo("T1");
        t.setOrderNo("O1");
        t.setTrainNo("G1234");
        t.setIdCardNo("11010519491231002X");
        t.setPrice(new BigDecimal("1058.00"));
        t.setStatus(1);
        when(ticketMapper.selectByTicketNo("T1")).thenReturn(t);

        var vo = ticketService.getByTicketNo("T1");
        assertNotNull(vo);
        assertEquals("110105********002X", vo.getIdCardNo());
    }
}
