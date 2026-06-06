package com.railway.order.manager;

import com.railway.common.model.R;
import com.railway.order.feign.StockFeignClient;
import com.railway.order.feign.dto.ConfirmStockDTO;
import com.railway.order.feign.dto.OccupyStockDTO;
import com.railway.order.feign.dto.ReleaseStockDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 库存编排：封装 Feign 调用 stock + 失败抛出。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStockManager {

    private final StockFeignClient stockFeignClient;

    public void occupy(String orderNo, String trainNo, java.time.LocalDate runDate,
                       String seatType, int num) {
        OccupyStockDTO dto = new OccupyStockDTO();
        dto.setOrderNo(orderNo);
        dto.setTrainNo(trainNo);
        dto.setRunDate(runDate);
        dto.setSeatType(seatType);
        dto.setNum(num);
        R<Void> r = stockFeignClient.occupy(dto);
        if (r == null || r.getCode() != 200) {
            throw new com.railway.common.exception.BizException(
                    com.railway.common.exception.ErrorCode.STOCK_NOT_ENOUGH,
                    r == null ? "stock.occupy 失败（无响应）" : r.getMessage());
        }
    }

    public void confirm(String orderNo, String trainNo, java.time.LocalDate runDate,
                         String seatType, int num) {
        ConfirmStockDTO dto = new ConfirmStockDTO();
        dto.setOrderNo(orderNo);
        dto.setTrainNo(trainNo);
        dto.setRunDate(runDate);
        dto.setSeatType(seatType);
        dto.setNum(num);
        R<Void> r = stockFeignClient.confirm(dto);
        if (r == null || r.getCode() != 200) {
            log.error("stock.confirm 失败 orderNo={} msg={}", orderNo, r == null ? "null" : r.getMessage());
        }
    }

    public void release(String orderNo, String trainNo, java.time.LocalDate runDate,
                        String seatType, int num) {
        ReleaseStockDTO dto = new ReleaseStockDTO();
        dto.setOrderNo(orderNo);
        dto.setTrainNo(trainNo);
        dto.setRunDate(runDate);
        dto.setSeatType(seatType);
        dto.setNum(num);
        R<Void> r = stockFeignClient.release(dto);
        if (r == null || r.getCode() != 200) {
            log.error("stock.release 失败 orderNo={} msg={}", orderNo, r == null ? "null" : r.getMessage());
        }
    }
}
