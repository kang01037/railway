package com.railway.trainstock.service;

import com.railway.trainstock.dto.request.ConfirmDTO;
import com.railway.trainstock.dto.request.OccupyDTO;
import com.railway.trainstock.dto.request.ReleaseDTO;
import com.railway.trainstock.vo.OccupyVO;

/**
 * 库存服务：预占 / 确认 / 释放。
 * <p>供 service-order 通过 Feign 调用，**全部 @AuthIgnore**（内部接口）。
 */
public interface StockService {

    /**
     * 预占库存。Redis 原子扣减 + 写 stock_flow + DB 乐观锁扣减。
     */
    OccupyVO occupy(OccupyDTO dto);

    /**
     * 确认扣减（支付成功）：仅写 stock_flow（delta=-num, bizType=2），
     * Redis / DB 均不再变动（已在 occupy 阶段扣减）。
     */
    void confirm(ConfirmDTO dto);

    /**
     * 释放库存（订单取消 / 支付失败）：Redis 原子回滚 + 写 stock_flow + DB 乐观锁回滚。
     */
    void release(ReleaseDTO dto);
}
