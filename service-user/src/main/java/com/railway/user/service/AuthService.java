package com.railway.user.service;

import com.railway.common.model.LoginUser;
import com.railway.user.dto.request.LoginDTO;
import com.railway.user.dto.request.RegisterDTO;
import com.railway.user.vo.LoginVO;

/**
 * 认证服务：注册 / 登录 / 登出 / 当前用户。
 */
public interface AuthService {

    /**
     * 注册新用户，返回 userId。
     */
    Long register(RegisterDTO dto);

    /**
     * 登录：校验密码 → 签发 JWT → 写 Redis 登录态 → 返回 token。
     */
    LoginVO login(LoginDTO dto);

    /**
     * 登出：清空 Redis 登录态（token 在 JWT 有效期内仍可解析，但可被业务侧检查拦截）。
     */
    void logout();

    /**
     * 获取当前登录用户（从 UserContext 取）。
     */
    LoginUser me();
}
