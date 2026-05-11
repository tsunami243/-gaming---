package org.example.bigevent.config;

import org.example.bigevent.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 关闭 CSRF（使用 JWT）
            .csrf(csrf -> csrf.disable())
            // 关闭 Session（无状态）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置权限
            .authorizeHttpRequests(auth -> auth
                // 公开接口
                .requestMatchers(HttpMethod.POST, "/user/login", "/user/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/article", "/article/detail", "/article/stats", "/article/heat").permitAll()
                .requestMatchers(HttpMethod.GET, "/category", "/category/detail").permitAll()
                .requestMatchers(HttpMethod.GET, "/comment/*/tree", "/comment/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/article-view/**").permitAll()
                // 轮播图需要 CONTENT_ADMIN
                .requestMatchers("/article/carousel").hasAnyRole("CONTENT_ADMIN", "SUPER_ADMIN")
                // 分类管理需要 CONTENT_ADMIN
                .requestMatchers(HttpMethod.POST, "/category").hasAnyRole("CONTENT_ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/category").hasAnyRole("CONTENT_ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/category").hasAnyRole("CONTENT_ADMIN", "SUPER_ADMIN")
                // 用户管理需要 SUPER_ADMIN
                .requestMatchers("/user/list", "/user/*/delete").hasRole("SUPER_ADMIN")
                // 其他接口需要登录
                .anyRequest().authenticated()
            )
            // 添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            // 认证失败返回 401
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
