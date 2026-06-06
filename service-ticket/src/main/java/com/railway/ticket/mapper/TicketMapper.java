package com.railway.ticket.mapper;

import com.railway.ticket.entity.TicketDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TicketMapper {

    int insert(TicketDO ticket);

    TicketDO selectById(@Param("id") Long id);

    TicketDO selectByTicketNo(@Param("ticketNo") String ticketNo);

    List<TicketDO> listByOrderNo(@Param("orderNo") String orderNo);

    int countByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 状态机：待支付 → 已出票（0 → 1）。返回受影响行数。
     */
    int confirmByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 状态机：待支付/已出票 → 已退（0/1 → 3）。返回受影响行数。
     */
    int cancelByOrderNo(@Param("orderNo") String orderNo);
}
