package com.railway.order.vo;

import lombok.Data;

import java.util.List;

/**
 * 订单详情：order + 票列表（票数据从 service-ticket 拿）。
 */
@Data
public class OrderDetailVO {

    private OrderVO order;

    /** 票列表（来自 service-ticket 调用的 VO） */
    private List<?> tickets;
}
