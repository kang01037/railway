package com.railway.common.model;

import com.railway.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统一返回体 R 单测：工厂方法 + isSuccess。
 */
class RTest {

    @Test
    void testOkWithData() {
        R<String> r = R.ok("hello");
        assertEquals(200, r.getCode());
        assertEquals("成功", r.getMessage());
        assertEquals("hello", r.getData());
        assertTrue(r.isSuccess());
    }

    @Test
    void testOkWithoutData() {
        R<String> r = R.ok();
        assertEquals(200, r.getCode());
        assertNull(r.getData());
        assertTrue(r.isSuccess());
    }

    @Test
    void testFailWithCode() {
        R<String> r = R.fail(404, "资源不存在");
        assertEquals(404, r.getCode());
        assertEquals("资源不存在", r.getMessage());
        assertNull(r.getData());
        assertFalse(r.isSuccess());
    }

    @Test
    void testFailWithErrorCode() {
        R<String> r = R.fail(ErrorCode.USER_NOT_FOUND);
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), r.getCode());
        assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), r.getMessage());
        assertFalse(r.isSuccess());
    }

    @Test
    void testFailWithErrorCodeAndMessage() {
        R<String> r = R.fail(ErrorCode.BAD_REQUEST, "id 必须正整数");
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), r.getCode());
        assertEquals("id 必须正整数", r.getMessage());
    }
}
