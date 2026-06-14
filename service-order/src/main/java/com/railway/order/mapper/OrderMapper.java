package com.railway.order.mapper;

import com.railway.order.entity.OrderDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface OrderMapper {

    int insert(OrderDO order);

    OrderDO selectById(@Param("id") Long id);

    OrderDO selectByOrderNo(@Param("orderNo") String orderNo);

    /** 按 userId 查所有订单（倒序，配合 PageHelper） */
    List<OrderDO> listByUserId(@Param("userId") Long userId);

    /**
     * 状态机：0 → 1 已支付。
     * @return 受影响行数（0 = 状态不对或不存在，幂等命中）
     */
    int confirmByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 状态机：0 → 2 用户主动取消（仅待支付可取消）。
     */
    int cancelByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 状态机：0/1 → 3 已退款（关单 / 自动关单）。
     */
    int refundByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 状态机：1 → 4 已完成（暂不主动调用，留接口）。
     */
    int completeByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 出票失败补偿：1 → 0 回滚到待支付。
     */
    int rollbackConfirm(@Param("orderNo") String orderNo);

    int countByUserId(@Param("userId") Long userId);
}
