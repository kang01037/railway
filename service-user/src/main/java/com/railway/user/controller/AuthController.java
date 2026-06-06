package com.railway.user.controller;

import com.railway.common.annotation.AuthIgnore;
import com.railway.common.model.LoginUser;
import com.railway.common.model.R;
import com.railway.user.dto.request.LoginDTO;
import com.railway.user.dto.request.RegisterDTO;
import com.railway.user.service.AuthService;
import com.railway.user.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册 / 登录 / 登出 / 当前用户。
 *
 * <p>通过 gateway 后真实路径：
 * <ul>
 *   <li>POST /api/user/auth/register</li>
 *   <li>POST /api/user/auth/login</li>
 *   <li>POST /api/user/auth/logout</li>
 *   <li>GET  /api/user/auth/me</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @AuthIgnore
    @PostMapping("/register")
    public R<Long> register(@Valid @RequestBody RegisterDTO dto) {
        Long userId = authService.register(dto);
        return R.ok(userId);
    }

    @AuthIgnore
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @GetMapping("/me")
    public R<LoginUser> me() {
        return R.ok(authService.me());
    }
}
