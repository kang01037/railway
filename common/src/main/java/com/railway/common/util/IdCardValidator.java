package com.railway.common.util;

import java.util.regex.Pattern;

/**
 * 身份证校验：18 位 + 校验位 + 生日合法性。
 */
public class IdCardValidator {

    private static final Pattern PATTERN = Pattern.compile(
            "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$");

    private static final int[] WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    public static boolean isValid(String idCard) {
        if (idCard == null || !PATTERN.matcher(idCard).matches()) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (idCard.charAt(i) - '0') * WEIGHT[i];
        }
        char expected = CHECK_CODES[sum % 11];
        char actual = idCard.charAt(17);
        return expected == Character.toUpperCase(actual);
    }

    /**
     * 脱敏：前 6 后 4，中间 8 个 *。
     */
    public static String mask(String idCard) {
        if (idCard == null || idCard.length() < 11) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    private IdCardValidator() {
    }
}
