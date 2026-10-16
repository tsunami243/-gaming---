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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 安全配置：无状态 JWT 鉴权 + 按路径/方法的授权规则。
 *
 * <p>关键设计：</p>
 * <ul>
 *   <li>关闭 CSRF 与 Session——认证完全依赖请求头中的 JWT，服务端不保存会话</li>
 *   <li>授权规则自上而下匹配，<b>具体规则必须写在通配规则之前</b>，例如
 *       {@code /api/v1/admin/**} 要排在 {@code /api/v1/articles/**} 之前，否则会被后者吞掉</li>
 *   <li>{@code /internal/**} 为 permitAll——该链路的防护由
 *       {@code IngestRequestBoundaryFilter} 的令牌校验负责，不在这里做</li>
 *   <li>认证失败统一返回 401（{@link HttpStatusEntryPoint}），不跳转登录页</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 定义过滤器链与授权规则。
     *
     * @param http Spring Security 的 HTTP 构建器
     * @return 构建好的过滤器链
     * @throws Exception 构建失败时抛出
     */
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
                // Legacy demo endpoints are deliberately disabled in production.
                .requestMatchers("/push/**").denyAll()

                // === 受保护操作（具体规则，放在通配前面） ===
                .requestMatchers("/user/list").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("CONTENT_ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/v1/admin/**").hasAnyRole("CONTENT_ADMIN", "SUPER_ADMIN")
                .requestMatchers("/internal/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/games", "/api/v1/articles/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/category").hasAnyRole("CONTENT_ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/category").hasAnyRole("CONTENT_ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/category").hasAnyRole("CONTENT_ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST, "/article/**").hasAnyRole("CONTENT_ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/article/**").hasAnyRole("CONTENT_ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/article/**").hasAnyRole("CONTENT_ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST, "/comment/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/comment/**").authenticated()
                // Legacy direct like-count mutation is superseded by comment-like and must not be public.
                .requestMatchers(HttpMethod.PUT, "/comment/**").denyAll()
                .requestMatchers("/upload").authenticated()

                // === 公开读接口（无方法限制，GET 被上面跳过自然命中这里） ===
                .requestMatchers("/article/**").permitAll()
                .requestMatchers("/category/**").permitAll()
                .requestMatchers("/comment/**").permitAll()

                .anyRequest().authenticated()
            )
            // 添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // 认证失败返回 401
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        return http.build();
    }

    /**
     * 暴露认证管理器，供登录接口手动触发认证流程。
     *
     * @param config Spring Security 的认证配置
     * @return 认证管理器
     * @throws Exception 获取失败时抛出
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 配置 CORS：仅放行本机来源（含任意端口），供前端开发服务器跨域访问。
     *
     * @return 生效于 {@code /**} 的 CORS 过滤器
     */
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
