package com.railway.common.constant;

/**
 * RabbitMQ 交换机 / 队列 / 路由 Key 常量。
 *
 * <p>延迟关单：{@code order.delay.queue} 配 TTL=15min + DLX=order.exchange, DLRK=order.cancel。
 */
public final class MqConstant {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    public static final String ORDER_PAID_QUEUE = "order.paid.queue";
    public static final String ORDER_CANCEL_QUEUE = "order.cancel.queue";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_CLOSE_QUEUE = "order.close.queue";

    public static final String RK_ORDER_PAID = "order.paid";
    public static final String RK_ORDER_CANCEL = "order.cancel";
    public static final String RK_ORDER_DELAY = "order.delay";
    public static final String RK_ORDER_CLOSE = "order.close";

    public static final String USER_EXCHANGE = "user.exchange";
    public static final String USER_REGISTERED_QUEUE = "user.registered.queue";
    public static final String RK_USER_REGISTERED = "user.registered";

    public static final String TRAIN_EXCHANGE = "train.exchange";
    public static final String TRAIN_SYNC_QUEUE = "train.sync.queue";
    public static final String RK_TRAIN_SYNC = "train.sync";

    private MqConstant() {
    }
}
