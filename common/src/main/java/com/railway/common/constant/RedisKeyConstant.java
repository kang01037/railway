package com.railway.common.constant;

/**
 * Redis key 命名空间常量。
 *
 * <p>命名规则：{@code 业务域:业务键[:子键]}，避免冲突。
 */
public final class RedisKeyConstant {

    public static final String IDEMPOTENT = "IDEMPOTENT:";
    public static final String USER_LOGIN = "USER_LOGIN:";
    public static final String STOCK = "STOCK:";
    public static final String SEAT_POOL = "SEAT_POOL:";
    public static final String SEAT_RESERVE = "SEAT_RESERVE:";
    public static final String TICKET_CACHE = "TICKET_CACHE:";
    public static final String ORDER_CACHE = "ORDER_CACHE:";

    private RedisKeyConstant() {
    }

    public static String stockKey(String trainNo, String runDate, String seatType) {
        return STOCK + trainNo + ":" + runDate + ":" + seatType;
    }

    public static String seatKey(String trainNo, String runDate, String seatType) {
        return SEAT_POOL + trainNo + ":" + runDate + ":" + seatType;
    }

    public static String seatReserveKey(String orderNo) {
        return SEAT_RESERVE + orderNo;
    }

    public static String userLoginKey(Long userId) {
        return USER_LOGIN + userId;
    }

    public static String ticketByOrderKey(String orderNo) {
        return TICKET_CACHE + "order:" + orderNo;
    }

    public static String ticketByTicketNoKey(String ticketNo) {
        return TICKET_CACHE + "ticket:" + ticketNo;
    }

    public static String orderByOrderNoKey(String orderNo) {
        return ORDER_CACHE + "orderNo:" + orderNo;
    }

    public static String orderByUserIdKey(Long userId, int pageNum) {
        return ORDER_CACHE + "userId:" + userId + ":page:" + pageNum;
    }
}
