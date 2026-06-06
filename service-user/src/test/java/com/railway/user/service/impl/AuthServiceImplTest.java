package com.railway.user.service.impl;

import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.util.JwtUtil;
import com.railway.common.util.SnowflakeIdWorker;
import com.railway.user.dto.request.LoginDTO;
import com.railway.user.dto.request.RegisterDTO;
import com.railway.user.entity.UserDO;
import com.railway.user.mapper.UserMapper;
import com.railway.user.vo.LoginVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthServiceImpl 单测：注册 / 登录 / 登出 / 鉴权查询。
 * <p>所有 mapper / Redis / Jwt / 雪花 / BCrypt 全部 mock。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private SnowflakeIdWorker snowflakeIdWorker;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private AuthServiceImpl authService;

    // ============== register ==============

    @Test
    void register_duplicateUsername_throws() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("dup");
        dto.setPassword("p");

        when(userMapper.countByUsername("dup")).thenReturn(1);

        BizException ex = assertThrows(BizException.class, () -> authService.register(dto));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS.getCode(), ex.getCode());
        verify(userMapper, never()).insert(any());
    }

    @Test
    void register_newUser_returnsId() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("newone");
        dto.setPassword("p");
        dto.setPhone("13800000000");

        when(userMapper.countByUsername("newone")).thenReturn(0);
        when(snowflakeIdWorker.nextId()).thenReturn(12345L);
        when(passwordEncoder.encode("p")).thenReturn("BCRYPT_HASH");

        Long id = authService.register(dto);
        assertEquals(12345L, id);
        verify(userMapper, times(1)).insert(any(UserDO.class));
    }

    // ============== login ==============

    @Test
    void login_userNotFound_throws() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("ghost");
        dto.setPassword("p");
        when(userMapper.selectByUsername("ghost")).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> authService.login(dto));
        assertEquals(ErrorCode.USER_PASSWORD_ERROR.getCode(), ex.getCode());
    }

    @Test
    void login_disabled_throws() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("alice");
        dto.setPassword("p");
        UserDO u = new UserDO();
        u.setId(1L);
        u.setUsername("alice");
        u.setStatus(0);  // 禁用
        when(userMapper.selectByUsername("alice")).thenReturn(u);

        BizException ex = assertThrows(BizException.class, () -> authService.login(dto));
        assertEquals(ErrorCode.USER_DISABLED.getCode(), ex.getCode());
    }

    @Test
    void login_wrongPassword_throws() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("bob");
        dto.setPassword("wrong");
        UserDO u = new UserDO();
        u.setId(2L);
        u.setUsername("bob");
        u.setPassword("HASHED");
        u.setStatus(1);
        when(userMapper.selectByUsername("bob")).thenReturn(u);
        when(passwordEncoder.matches("wrong", "HASHED")).thenReturn(false);

        BizException ex = assertThrows(BizException.class, () -> authService.login(dto));
        assertEquals(ErrorCode.USER_PASSWORD_ERROR.getCode(), ex.getCode());
        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void login_success_returnsVoAndStoresRedis() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        LoginDTO dto = new LoginDTO();
        dto.setUsername("carol");
        dto.setPassword("p");
        UserDO u = new UserDO();
        u.setId(3L);
        u.setUsername("carol");
        u.setPassword("HASHED");
        u.setStatus(1);
        u.setUserType(0);
        when(userMapper.selectByUsername("carol")).thenReturn(u);
        when(passwordEncoder.matches("p", "HASHED")).thenReturn(true);
        when(jwtUtil.getExpireMinutes()).thenReturn(120L);
        when(jwtUtil.generate(any())).thenReturn("TOK.xxx.yyy");

        LoginVO vo = authService.login(dto);
        assertEquals("TOK.xxx.yyy", vo.getToken());
        assertEquals(3L, vo.getUserId());
        assertEquals("carol", vo.getUsername());
        assertTrue(vo.getRoles().contains("USER"));
        assertNotNull(vo.getExpireAt());
        verify(valueOps, times(1)).set(anyString(), eq("TOK.xxx.yyy"), any(Duration.class));
    }

    @Test
    void login_adminUser_hasAdminRole() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("p");
        UserDO u = new UserDO();
        u.setId(99L);
        u.setUsername("admin");
        u.setPassword("HASHED");
        u.setStatus(1);
        u.setUserType(1);  // 管理员
        when(userMapper.selectByUsername("admin")).thenReturn(u);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.getExpireMinutes()).thenReturn(60L);
        when(jwtUtil.generate(any())).thenReturn("ADMIN_TOK");

        LoginVO vo = authService.login(dto);
        Set<String> roles = new HashSet<>(vo.getRoles());
        assertTrue(roles.contains("USER"));
        assertTrue(roles.contains("ADMIN"));
    }
}
