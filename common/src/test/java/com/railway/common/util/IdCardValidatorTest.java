package com.railway.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 身份证校验单测：18 位 + 校验位 + 脱敏。
 */
class IdCardValidatorTest {

    /** 一个合法身份证（末位校验位 = X）。可由网上"身份证号生成器"得到。 */
    private static final String VALID = "11010519491231002X";
    /** 另一个合法身份证（末位校验位 = 0）。 */
    private static final String VALID_2 = "110101199003073570";

    @Test
    void testValidIdCard() {
        assertTrue(IdCardValidator.isValid(VALID));
        assertTrue(IdCardValidator.isValid(VALID_2));
    }

    @Test
    void testInvalidIdCard() {
        assertFalse(IdCardValidator.isValid(null));
        assertFalse(IdCardValidator.isValid(""));
        assertFalse(IdCardValidator.isValid("12345"));                              // 太短
        assertFalse(IdCardValidator.isValid("110105194912310021"));                 // 末位校验错
        assertFalse(IdCardValidator.isValid("110105194913310021"));                 // 月份 13
        assertFalse(IdCardValidator.isValid("110105194912320021"));                 // 日期 32
        assertFalse(IdCardValidator.isValid("01010519491231002X"));                 // 首位 0（省码错）
    }

    @Test
    void testMask() {
        String masked = IdCardValidator.mask(VALID);
        assertEquals(18, masked.length());
        assertTrue(masked.startsWith("110105"));
        assertTrue(masked.endsWith("002X"));
        assertTrue(masked.contains("********"));
    }

    @Test
    void testMaskShortOrNull() {
        assertSame(null, IdCardValidator.mask(null));
        assertEquals("12345", IdCardValidator.mask("12345"));   // 不足 11 位原样返回
    }
}
