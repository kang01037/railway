package com.railway.common.auto;

import com.railway.common.aspect.RequireRoleAspect;
import com.railway.common.config.JacksonConfig;
import com.railway.common.config.MyBatisConfig;
import com.railway.common.config.RedisConfig;
import com.railway.common.config.RedisLuaConfig;
import com.railway.common.config.SecurityConfig;
import com.railway.common.config.WebConfig;
import com.railway.common.interceptor.AuthInterceptor;
import com.railway.common.util.JwtUtil;
import com.railway.common.util.SnowflakeIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * common 模块的 Spring Boot 自动装配入口。
 *
 * <p>条件：仅在 servlet Web 应用中生效（gateway 是 webflux，会被自动跳过）。
 * 通过 META-INF/spring/...AutoConfiguration.imports 被 Spring Boot 发现。
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({
        JacksonConfig.class,
        MyBatisConfig.class,
        RedisConfig.class,
        RedisLuaConfig.class,
        SecurityConfig.class,
        WebConfig.class
})
public class WebAutoConfiguration {

    static {
        log.info("[common] WebAutoConfiguration loaded");
    }

    /**
     * JwtUtil：{@code jwt.secret} / {@code jwt.expire-minutes} / {@code jwt.issuer} 从 yml 绑定。
     */
    @Bean
    @ConfigurationProperties(prefix = "jwt")
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }

    /**
     * SnowflakeIdWorker：{@code snowflake.worker-id} 从 yml 绑定。
     */
    @Bean
    @ConfigurationProperties(prefix = "snowflake")
    public SnowflakeIdWorker snowflakeIdWorker() {
        return new SnowflakeIdWorker();
    }

    /**
     * 鉴权拦截器。
     */
    @Bean
    public AuthInterceptor authInterceptor(JwtUtil jwtUtil) {
        return new AuthInterceptor(jwtUtil);
    }

    /**
     * 角色切面（@Aspect 注解 + 容器管理，自动织入）。
     */
    @Bean
    public RequireRoleAspect requireRoleAspect() {
        return new RequireRoleAspect();
    }

    /**
     * 把 AuthInterceptor 注册到 Spring MVC 拦截器链。
     * 用独立 @Bean 实现 WebMvcConfigurer，避开"自身实现 WebMvcConfigurer + 自身 @Bean"循环依赖。
     */
    @Bean
    public WebMvcConfigurer authInterceptorConfigurer(AuthInterceptor authInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(authInterceptor)
                        .addPathPatterns("/**")
                        .excludePathPatterns("/error");
            }
        };
    }
}
