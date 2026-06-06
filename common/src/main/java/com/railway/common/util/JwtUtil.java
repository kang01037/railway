package com.railway.common.util;

import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.model.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JWT 工具：签发、解析。
 *
 * <p>使用 HS256 对称加密，密钥至少 32 字节。配置项：
 * <ul>
 *   <li>{@code jwt.secret} - 密钥（≥32 字节）</li>
 *   <li>{@code jwt.expire-minutes} - 过期分钟数，默认 120</li>
 *   <li>{@code jwt.issuer} - 签发者，默认 railway</li>
 * </ul>
 */
@Slf4j
public class JwtUtil {

    private String secret = "0123456789abcdef0123456789abcdef0123456789abcdef";
    private long expireMinutes = 120;
    private String issuer = "railway";

    private volatile SecretKey secretKey;

    public void setSecret(String secret) {
        if (secret == null) {
            throw new BizException(ErrorCode.SERVER_ERROR, "jwt.secret 不能为空");
        }
        int len = secret.getBytes(StandardCharsets.UTF_8).length;
        if (len < 32) {
            throw new BizException(ErrorCode.SERVER_ERROR,
                    "jwt.secret 至少 32 字节，当前 " + len);
        }
        this.secret = secret;
        this.secretKey = null;
    }

    public void setExpireMinutes(long expireMinutes) {
        this.expireMinutes = expireMinutes;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSecret() {
        return secret;
    }

    public long getExpireMinutes() {
        return expireMinutes;
    }

    public String getIssuer() {
        return issuer;
    }

    private SecretKey getKey() {
        SecretKey k = secretKey;
        if (k == null) {
            synchronized (this) {
                k = secretKey;
                if (k == null) {
                    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
                    if (bytes.length < 32) {
                        throw new BizException(ErrorCode.SERVER_ERROR,
                                "jwt.secret 至少 32 字节，当前 " + bytes.length);
                    }
                    k = Keys.hmacShaKeyFor(bytes);
                    secretKey = k;
                }
            }
        }
        return k;
    }

    /**
     * 签发 token。
     */
    public String generate(LoginUser user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getUserId())
                .claim("roles", user.getRoles())
                .issuer(issuer)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireMinutes * 60_000L))
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析 token，失败抛 BizException(UNAUTHORIZED)。
     */
    public LoginUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            LoginUser user = new LoginUser();
            Number userIdNum = claims.get("userId", Number.class);
            user.setUserId(userIdNum == null ? null : userIdNum.longValue());
            user.setUsername(claims.getSubject());
            @SuppressWarnings("unchecked")
            List<String> rolesList = claims.get("roles", List.class);
            if (rolesList != null) {
                user.setRoles(new HashSet<>(rolesList));
            } else {
                user.setRoles(new HashSet<>());
            }
            return user;
        } catch (ExpiredJwtException e) {
            log.warn("JWT 已过期");
            throw new BizException(ErrorCode.UNAUTHORIZED, "Token 已过期");
        } catch (JwtException e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            throw new BizException(ErrorCode.UNAUTHORIZED, "Token 无效");
        }
    }
}
