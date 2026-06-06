package com.railway.user.service.impl;

import com.railway.common.constant.RedisKeyConstant;
import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.model.LoginUser;
import com.railway.common.util.JwtUtil;
import com.railway.common.util.SnowflakeIdWorker;
import com.railway.common.util.UserContext;
import com.railway.user.dto.request.LoginDTO;
import com.railway.user.dto.request.RegisterDTO;
import com.railway.user.entity.UserDO;
import com.railway.user.mapper.UserMapper;
import com.railway.user.service.AuthService;
import com.railway.user.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterDTO dto) {
        // 1. 用户名唯一
        if (userMapper.countByUsername(dto.getUsername()) > 0) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 2. 构建用户实体
        UserDO user = new UserDO();
        user.setId(snowflakeIdWorker.nextId());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRealName(dto.getRealName());
        user.setUserType(0);   // 默认普通用户
        user.setStatus(1);     // 正常
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 3. 写库
        userMapper.insert(user);
        log.info("用户注册成功 username={} userId={}", user.getUsername(), user.getId());
        return user.getId();
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 查用户
        UserDO user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            // 用户名错 / 密码错 统一返回，不暴露用户是否存在
            throw new BizException(ErrorCode.USER_PASSWORD_ERROR);
        }
        // 2. 状态校验
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
        // 3. 密码校验
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.USER_PASSWORD_ERROR);
        }

        // 4. 构造角色
        Set<String> roles = new HashSet<>();
        roles.add("USER");
        if (user.getUserType() != null && user.getUserType() == 1) {
            roles.add("ADMIN");
        }

        // 5. 签发 JWT
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), roles);
        String token = jwtUtil.generate(loginUser);

        // 6. 写 Redis 登录态（用于强制下线 / 在线人数统计）
        Duration expire = Duration.ofMinutes(jwtUtil.getExpireMinutes());
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstant.userLoginKey(user.getId()),
                token,
                expire);

        // 7. 过期时间戳
        long expireAt = System.currentTimeMillis() + expire.toMillis();

        log.info("用户登录 username={} userId={} roles={}", user.getUsername(), user.getId(), roles);
        return new LoginVO(token, user.getId(), user.getUsername(), roles, expireAt);
    }

    @Override
    public void logout() {
        Long userId = UserContext.mustCurrentUserId();
        Boolean deleted = stringRedisTemplate.delete(RedisKeyConstant.userLoginKey(userId));
        log.info("用户登出 userId={} deleted={}", userId, deleted);
    }

    @Override
    public LoginUser me() {
        LoginUser user = UserContext.current();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
