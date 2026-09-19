package com.cet6.sprint.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 通用 Bean 配置
 */
@Configuration
public class BeanConfig {

    /**
     * 密码编码器
     * <p>
     * 只引入 spring-security-crypto 这一个轻量模块，而不是整个 Spring Security：
     * 本项目用 JWT + 拦截器做鉴权，不需要 Security 的过滤器链，
     * 引全家桶反而要额外写配置关掉默认登录页和 CSRF。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
