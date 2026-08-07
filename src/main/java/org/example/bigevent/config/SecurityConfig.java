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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

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
                // === 公开接口（无方法限制） ===
                .requestMatchers("/test/**").permitAll()
                .requestMatchers("/user/login", "/user/register").permitAll()
                .requestMatchers("/article-view/**").permitAll()
                // 推送平台 demo 模块（生产应移入认证链）
                .requestMatchers("/push/**").permitAll()

                // === 受保护操作（具体规则，放在通配前面） ===
                .requestMatchers(HttpMethod.POST, "/category").hasAnyRole("CONTENT_ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/category").hasAnyRole("CONTENT_ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/category").hasAnyRole("CONTENT_ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST, "/article/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/article/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/article/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/comment/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/comment/**").authenticated()
                .requestMatchers("/upload").authenticated()

                // === 公开读接口（无方法限制，GET 被上面跳过自然命中这里） ===
                .requestMatchers("/article/**").permitAll()
                .requestMatchers("/category/**").permitAll()
                .requestMatchers("/comment/**").permitAll()

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

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
