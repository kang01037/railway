package com.railway.trainstock.service.impl;

import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.util.SnowflakeIdWorker;
import com.railway.trainstock.dto.request.ConfirmDTO;
import com.railway.trainstock.dto.request.OccupyDTO;
import com.railway.trainstock.dto.request.ReleaseDTO;
import com.railway.trainstock.entity.TrainSeatStockDO;
import com.railway.trainstock.mapper.StockFlowMapper;
import com.railway.trainstock.mapper.TrainSeatStockMapper;
import com.railway.trainstock.manager.StockManager;
import com.railway.trainstock.vo.OccupyVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * StockServiceImpl 单测：occupy / confirm / release 关键分支。
 * <p>并发控制方案：Redis 分布式锁 + Lua 脚本（StockManager 内部 mock）。
 */
@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock private StockManager stockManager;
    @Mock private TrainSeatStockMapper stockMapper;
    @Mock private StockFlowMapper stockFlowMapper;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private SnowflakeIdWorker snowflakeIdWorker;

    @InjectMocks private StockServiceImpl stockService;

    private TrainSeatStockDO sampleStock(int remain, int version) {
        TrainSeatStockDO s = new TrainSeatStockDO();
        s.setId(1L);
        s.setTrainNo("G1234");
        s.setRunDate(LocalDate.of(2026, 6, 10));
        s.setSeatType("SECOND");
        s.setTotal(120);
        s.setRemain(remain);
        s.setVersion(version);
        return s;
    }

    // ============== occupy ==============

    @Test
    void occupy_happyPath_writesFlowAndDecrementsDb() {
        OccupyDTO dto = new OccupyDTO();
        dto.setOrderNo("O1");
        dto.setTrainNo("G1234");
        dto.setRunDate(LocalDate.of(2026, 6, 10));
        dto.setSeatType("SECOND");
        dto.setNum(2);

        TrainSeatStockDO db = sampleStock(50, 0);
        when(stockMapper.selectByKey("G1234", dto.getRunDate(), "SECOND")).thenReturn(db);
        when(stockManager.occupy(anyString(), any(), anyString(), eq(2))).thenReturn(1L);
        when(stockManager.getCurrent(anyString(), any(), anyString())).thenReturn(48L);
        when(stockMapper.decreaseRemain("G1234", dto.getRunDate(), "SECOND", 2)).thenReturn(1);
        when(snowflakeIdWorker.nextId()).thenReturn(999L);

        OccupyVO vo = stockService.occupy(dto);
        assertNotNull(vo);
        assertEquals(48L, vo.getRemainAfter());
        verify(stockFlowMapper, times(1)).insert(any());   // 写流水
        verify(stockMapper, times(1)).decreaseRemain("G1234", dto.getRunDate(), "SECOND", 2);
    }

    @Test
    void occupy_stockNotFound_throws() {
        OccupyDTO dto = new OccupyDTO();
        dto.setOrderNo("O1");
        dto.setTrainNo("G-NONE");
        dto.setRunDate(LocalDate.of(2026, 6, 10));
        dto.setSeatType("SECOND");
        dto.setNum(1);
        when(stockMapper.selectByKey("G-NONE", dto.getRunDate(), "SECOND")).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> stockService.occupy(dto));
        assertEquals(ErrorCode.STOCK_NOT_FOUND.getCode(), ex.getCode());
        verify(stockFlowMapper, never()).insert(any());
    }

    @Test
    void occupy_luaReturnsZero_throwsNotEnough() {
        OccupyDTO dto = new OccupyDTO();
        dto.setOrderNo("O1");
        dto.setTrainNo("G1234");
        dto.setRunDate(LocalDate.of(2026, 6, 10));
        dto.setSeatType("SECOND");
        dto.setNum(10);
        when(stockMapper.selectByKey(anyString(), any(), anyString())).thenReturn(sampleStock(5, 0));
        when(stockManager.occupy(anyString(), any(), anyString(), eq(10))).thenReturn(0L);

        BizException ex = assertThrows(BizException.class, () -> stockService.occupy(dto));
        assertEquals(ErrorCode.STOCK_NOT_ENOUGH.getCode(), ex.getCode());
        verify(stockFlowMapper, never()).insert(any());
    }

    @Test
    void occupy_lockTimeout_throwsConflict() {
        OccupyDTO dto = new OccupyDTO();
        dto.setOrderNo("O1");
        dto.setTrainNo("G1234");
        dto.setRunDate(LocalDate.of(2026, 6, 10));
        dto.setSeatType("SECOND");
        dto.setNum(2);
        when(stockMapper.selectByKey(anyString(), any(), anyString())).thenReturn(sampleStock(50, 0));
        when(stockManager.occupy(anyString(), any(), anyString(), eq(2))).thenReturn(-2L);

        BizException ex = assertThrows(BizException.class, () -> stockService.occupy(dto));
        assertEquals(ErrorCode.STOCK_VERSION_CONFLICT.getCode(), ex.getCode());
        verify(stockFlowMapper, never()).insert(any());
    }

    @Test
    void occupy_dbDecreaseFails_rollsBackRedis() {
        OccupyDTO dto = new OccupyDTO();
        dto.setOrderNo("O1");
        dto.setTrainNo("G1234");
        dto.setRunDate(LocalDate.of(2026, 6, 10));
        dto.setSeatType("SECOND");
        dto.setNum(2);
        when(stockMapper.selectByKey(anyString(), any(), anyString())).thenReturn(sampleStock(50, 0));
        when(stockManager.occupy(anyString(), any(), anyString(), eq(2))).thenReturn(1L);
        when(stockMapper.decreaseRemain(anyString(), any(), anyString(), eq(2))).thenReturn(0);

        BizException ex = assertThrows(BizException.class, () -> stockService.occupy(dto));
        assertEquals(ErrorCode.STOCK_NOT_ENOUGH.getCode(), ex.getCode());
        // 关键：回滚 Redis 释放同样数量
        verify(stockManager, times(1)).release(anyString(), any(), anyString(), eq(2));
    }

    // ============== confirm ==============

    @Test
    void confirm_writesFlowButNoRedisChange() {
        ConfirmDTO dto = new ConfirmDTO();
        dto.setOrderNo("O1");
        dto.setTrainNo("G1234");
        dto.setRunDate(LocalDate.of(2026, 6, 10));
        dto.setSeatType("SECOND");
        dto.setNum(2);
        when(snowflakeIdWorker.nextId()).thenReturn(1L);

        stockService.confirm(dto);

        ArgumentCaptor<com.railway.trainstock.entity.StockFlowDO> cap =
                ArgumentCaptor.forClass(com.railway.trainstock.entity.StockFlowDO.class);
        verify(stockFlowMapper, times(1)).insert(cap.capture());
        assertEquals(-2, cap.getValue().getDelta());
        assertEquals(2, cap.getValue().getBizType());
        // confirm 不调 Redis
        verify(stockManager, never()).occupy(anyString(), any(), anyString(), anyInt());
        verify(stockManager, never()).release(anyString(), any(), anyString(), anyInt());
    }

    // ============== release ==============

    @Test
    void release_happyPath_writesFlowAndIncrementsDb() {
        ReleaseDTO dto = new ReleaseDTO();
        dto.setOrderNo("O1");
        dto.setTrainNo("G1234");
        dto.setRunDate(LocalDate.of(2026, 6, 10));
        dto.setSeatType("SECOND");
        dto.setNum(2);
        when(stockMapper.selectByKey(anyString(), any(), anyString())).thenReturn(sampleStock(48, 1));
        when(stockManager.release(anyString(), any(), anyString(), eq(2))).thenReturn(1L);
        when(stockMapper.increaseRemain(anyString(), any(), anyString(), eq(2))).thenReturn(1);
        when(snowflakeIdWorker.nextId()).thenReturn(2L);

        stockService.release(dto);

        ArgumentCaptor<com.railway.trainstock.entity.StockFlowDO> cap =
                ArgumentCaptor.forClass(com.railway.trainstock.entity.StockFlowDO.class);
        verify(stockFlowMapper, times(1)).insert(cap.capture());
        assertEquals(2, cap.getValue().getDelta());
        assertEquals(3, cap.getValue().getBizType());
    }

    @Test
    void release_lockTimeout_throwsConflict() {
        ReleaseDTO dto = new ReleaseDTO();
        dto.setOrderNo("O1");
        dto.setTrainNo("G1234");
        dto.setRunDate(LocalDate.of(2026, 6, 10));
        dto.setSeatType("SECOND");
        dto.setNum(2);
        when(stockMapper.selectByKey(anyString(), any(), anyString())).thenReturn(sampleStock(48, 1));
        when(stockManager.release(anyString(), any(), anyString(), eq(2))).thenReturn(-2L);

        BizException ex = assertThrows(BizException.class, () -> stockService.release(dto));
        assertEquals(ErrorCode.STOCK_VERSION_CONFLICT.getCode(), ex.getCode());
        verify(stockFlowMapper, never()).insert(any());
    }

    @Test
    void release_stockNotFound_throws() {
        ReleaseDTO dto = new ReleaseDTO();
        dto.setOrderNo("O1");
        dto.setTrainNo("G-NONE");
        dto.setRunDate(LocalDate.of(2026, 6, 10));
        dto.setSeatType("SECOND");
        dto.setNum(1);
        when(stockMapper.selectByKey("G-NONE", dto.getRunDate(), "SECOND")).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> stockService.release(dto));
        assertEquals(ErrorCode.STOCK_NOT_FOUND.getCode(), ex.getCode());
        verify(stockFlowMapper, never()).insert(any());
    }
}
