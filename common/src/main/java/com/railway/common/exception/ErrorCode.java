package com.railway.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码。
 *
 * <p>分类：
 * <ul>
 *   <li>200/4xx/5xx - 通用</li>
 *   <li>1000-1099 - 通用业务</li>
 *   <li>1100-1199 - 用户域</li>
 *   <li>1200-1299 - 乘车人</li>
 *   <li>1300-1399 - 车次</li>
 *   <li>2000-2099 - 库存</li>
 *   <li>3000-3099 - 订单</li>
 *   <li>4000-4099 - 支付</li>
 *   <li>5000-5099 - 票</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    OK(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不被允许"),
    CONFLICT(409, "资源冲突"),
    SERVER_ERROR(500, "服务器内部错误"),

    BIZ_ERROR(1000, "业务异常"),
    IDEMPOTENT_REPEAT(1001, "重复请求"),

    USER_NOT_FOUND(1101, "用户不存在"),
    USER_PASSWORD_ERROR(1102, "用户名或密码错误"),
    USER_ALREADY_EXISTS(1103, "用户已存在"),
    USER_DISABLED(1104, "用户已被禁用"),
    USER_NOT_LOGIN(1105, "用户未登录"),

    PASSENGER_NOT_FOUND(1201, "乘车人不存在"),

    TRAIN_NOT_FOUND(1301, "车次不存在"),
    SEAT_TYPE_INVALID(1302, "座位类型无效"),

    STOCK_NOT_ENOUGH(2001, "库存不足"),
    STOCK_OCCUPY_FAILED(2002, "库存预占失败"),
    STOCK_NOT_FOUND(2003, "座位类型库存不存在"),
    STOCK_VERSION_CONFLICT(2004, "库存版本冲突"),

    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_EXPIRED(3002, "订单已过期"),
    ORDER_ALREADY_PAID(3003, "订单已支付"),
    ORDER_CANCEL_FAILED(3004, "订单取消失败"),
    ORDER_CREATE_FAILED(3005, "订单创建失败"),

    PAY_FAILED(4001, "支付失败"),
    PAY_NOT_FOUND(4002, "支付单不存在"),
    PAY_AMOUNT_ERROR(4003, "支付金额错误"),

    TICKET_ISSUE_FAILED(5001, "出票失败"),
    TICKET_NOT_FOUND(5002, "票不存在");

    private final int code;
    private final String message;
}
