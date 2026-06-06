package com.railway.common.util;

import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.model.LoginUser;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT 单元测试：签发 / 解析往返、过期、非法 token。
 */
class JwtUtilTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void testGenerateAndParse() {
        JwtUtil jwt = new JwtUtil();
        jwt.setSecret(SECRET);
        jwt.setExpireMinutes(120);

        LoginUser user = new LoginUser();
        user.setUserId(123L);
        user.setUsername("testuser");
        Set<String> roles = new HashSet<>();
        roles.add("USER");
        roles.add("ADMIN");
        user.setRoles(roles);

        String token = jwt.generate(user);
        assertNotNull(token);
        assertTrue(token.length() > 0);

        LoginUser parsed = jwt.parse(token);
        assertEquals(123L, parsed.getUserId());
        assertEquals("testuser", parsed.getUsername());
        assertEquals(2, parsed.getRoles().size());
        assertTrue(parsed.getRoles().contains("USER"));
        assertTrue(parsed.getRoles().contains("ADMIN"));
    }

    @Test
    void testInvalidToken() {
        JwtUtil jwt = new JwtUtil();
        jwt.setSecret(SECRET);
        BizException ex = assertThrows(BizException.class, () -> jwt.parse("invalid.token.here"));
        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    @Test
    void testShortSecretThrows() {
        JwtUtil jwt = new JwtUtil();
        assertThrows(BizException.class, () -> jwt.setSecret("short"));
    }

    @Test
    void testExpiredToken() {
        JwtUtil jwt = new JwtUtil();
        jwt.setSecret(SECRET);
        jwt.setExpireMinutes(-1);

        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setUsername("u");
        user.setRoles(new HashSet<>());

        String token = jwt.generate(user);
        BizException ex = assertThrows(BizException.class, () -> jwt.parse(token));
        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
    }
}
