package org.example.bigevent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 口令加密配置：统一提供 BCrypt 编码器。
 *
 * <p>BCrypt 自带随机盐且计算成本可调，优于 MD5 等快速摘要算法；
 * 项目内的历史 MD5 口令由 {@code Md5Util} 单独兼容，不走本 Bean。</p>
 */
@Configuration
public class PasswordConfig {

    /**
     * 提供口令编码器（BCrypt）。
     *
     * @return BCrypt 编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
