package com.railway.common.exception;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ErrorCode 单测：code 不重复、message 非空、按段分组正确。
 */
class ErrorCodeTest {

    @Test
    void testAllCodesDistinct() {
        Set<Integer> seen = new HashSet<>();
        for (ErrorCode ec : ErrorCode.values()) {
            assertTrue(seen.add(ec.getCode()),
                    "重复 code: " + ec + " code=" + ec.getCode());
        }
    }

    @Test
    void testAllMessagesNonEmpty() {
        for (ErrorCode ec : ErrorCode.values()) {
            assertTrue(ec.getMessage() != null && !ec.getMessage().isBlank(),
                    "message 为空: " + ec);
        }
    }

    @Test
    void testSegmentsAreCorrect() {
        // 抽样验证段位归类
        assertEquals(1100, ErrorCode.USER_NOT_FOUND.getCode() / 100 * 100);   // 1100-1199 用户域
        assertEquals(1200, ErrorCode.PASSENGER_NOT_FOUND.getCode() / 100 * 100); // 1200-1299 乘车人
        assertEquals(1300, ErrorCode.TRAIN_NOT_FOUND.getCode() / 100 * 100);  // 1300-1399 车次
        assertEquals(2000, ErrorCode.STOCK_NOT_ENOUGH.getCode() / 100 * 100);  // 2000-2099 库存
        assertEquals(3000, ErrorCode.ORDER_NOT_FOUND.getCode() / 100 * 100);   // 3000-3099 订单
        assertEquals(4000, ErrorCode.PAY_FAILED.getCode() / 100 * 100);        // 4000-4099 支付
        assertEquals(5000, ErrorCode.TICKET_ISSUE_FAILED.getCode() / 100 * 100); // 5000-5099 票
    }

    @Test
    void testGenericCodes() {
        assertEquals(200, ErrorCode.OK.getCode());
        assertEquals(400, ErrorCode.BAD_REQUEST.getCode());
        assertEquals(401, ErrorCode.UNAUTHORIZED.getCode());
        assertEquals(403, ErrorCode.FORBIDDEN.getCode());
        assertEquals(500, ErrorCode.SERVER_ERROR.getCode());
        assertEquals(1001, ErrorCode.IDEMPOTENT_REPEAT.getCode());
    }
}
