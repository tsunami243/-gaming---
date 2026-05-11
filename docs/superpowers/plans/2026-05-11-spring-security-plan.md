# Spring Security 整合实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将项目从自定义JWT拦截器迁移到Spring Security，实现用户认证和RBAC权限分级

**Architecture:** 使用Spring Security Filter Chain处理JWT认证，BCrypt加密密码，RBAC实现权限控制。原有JWT Token格式保留，渐进式迁移。

**Tech Stack:** Spring Security 6.x, BCrypt, JWT (jjwt), MySQL, Redis

---

## 文件结构

```
src/main/java/org/example/bigevent/
├── config/
│   └── SecurityConfig.java              # Spring Security配置
├── security/
│   ├── JwtAuthenticationFilter.java      # JWT认证过滤器
│   └── UserDetailsServiceImpl.java       # 用户认证服务
├── config/
│   └── PasswordConfig.java               # BCrypt密码配置
├── pojo/User.java                       # 实现UserDetails接口
├── service/impl/UserServiceImpl.java     # 使用BCrypt加密
├── Interceptors/LoginInterceptor.java    # 删除（迁移到Security）
├── WebConfig.java                       # 删除拦截器配置
└── utils/ThreadLocalUtil.java            # 保留（兼容现有代码）
```

---

## 实现任务

### Task 1: 添加 Spring Security 依赖

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 添加 Spring Security 依赖到 pom.xml**

在 `</dependencies>` 前添加：
```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Run: `cd gaming && ./mvnw clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: 提交**
```bash
git add pom.xml && git commit -m "feat: 添加 Spring Security 依赖"
```

---

### Task 2: 创建 PasswordConfig 密码配置类

**Files:**
- Create: `src/main/java/org/example/bigevent/config/PasswordConfig.java`

- [ ] **Step 1: 创建 BCrypt 密码配置**

```java
package org.example.bigevent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

Run: `cd gaming && ./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: 提交**
```bash
git add src/main/java/org/example/bigevent/config/PasswordConfig.java && git commit -m "feat: 添加 BCrypt 密码编码器配置"
```

---

### Task 3: 修改 User 实体实现 UserDetails 接口

**Files:**
- Modify: `src/main/java/org/example/bigevent/pojo/User.java`

- [ ] **Step 1: 修改 User.java**

将 `public class User` 改为 `public class User implements UserDetails`

添加必要方法：
```java
// 实现 UserDetails 接口的方法
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singletonList(new SimpleGrantedAuthority(role)); // role 字段存储 ROLE_xxx
}

@Override
public boolean isAccountNonExpired() { return true; }

@Override
public boolean isAccountNonLocked() { return true; }

@Override
public boolean isCredentialsNonExpired() { return true; }

@Override
public boolean isEnabled() { return true; }
```

添加 import：
```java
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.Collection;
```

Run: `cd gaming && ./mvnw compile`
Expected: BUILD SUCCESS（可能有未实现方法错误，正常）

- [ ] **Step 2: 提交**
```bash
git add src/main/java/org/example/bigevent/pojo/User.java && git commit -m "feat: User 实体实现 UserDetails 接口"
```

---

### Task 4: 创建 UserDetailsServiceImpl

**Files:**
- Create: `src/main/java/org/example/bigevent/security/UserDetailsServiceImpl.java`

- [ ] **Step 1: 创建 UserDetailsServiceImpl**

```java
package org.example.bigevent.security;

import org.example.bigevent.mapper.UserMapper;
import org.example.bigevent.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService, UserDetailsPasswordService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户名不存在");
        }
        return user;
    }

    @Override
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        return user;
    }
}
```

- [ ] **Step 2: 创建 UserMapper**

检查 `src/main/java/org/example/bigevent/mapper/UserMapper.java` 是否已有 `findByUsername` 方法。

如果不存在，在 UserMapper.java 添加：
```java
@Select("select * from user where username = #{username}")
User findByUsername(@Param("username") String username);
```

Run: `cd gaming && ./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**
```bash
git add src/main/java/org/example/bigevent/security/UserDetailsServiceImpl.java
git add src/main/java/org/example/bigevent/mapper/UserMapper.java
git commit -m "feat: 创建 UserDetailsServiceImpl 实现"
```

---

### Task 5: 创建 JwtAuthenticationFilter

**Files:**
- Create: `src/main/java/org/example/bigevent/security/JwtAuthenticationFilter.java`

- [ ] **Step 1: 创建 JwtAuthenticationFilter**

```java
package org.example.bigevent.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.bigevent.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String KEY = "tsunami1234567890abcdefghijklmnopqrstuvwxyz";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(KEY.getBytes());

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = request.getHeader("Authorization");

        if (token != null && !token.isEmpty()) {
            try {
                Claims claims = JwtUtil.parseToken(token);
                Integer userId = (Integer) claims.get("userId");
                String username = (String) claims.get("username");

                // 从 Redis 验证 token
                String redisToken = stringRedisTemplate.opsForValue().get("token" + userId);
                if (redisToken != null && redisToken.equals(token)) {
                    User user = new User();
                    user.setId(userId);
                    user.setUsername(username);
                    user.setPassword(claims.get("password", String.class)); // 获取密码用于认证

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException e) {
                // token 无效，继续过滤链（会返回未认证）
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

Run: `cd gaming && ./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: 提交**
```bash
git add src/main/java/org/example/bigevent/security/JwtAuthenticationFilter.java && git commit -m "feat: 创建 JWT 认证过滤器"
```

---

### Task 6: 创建 SecurityConfig 配置类

**Files:**
- Create: `src/main/java/org/example/bigevent/config/SecurityConfig.java`

- [ ] **Step 1: 创建 SecurityConfig**

```java
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
```

Run: `cd gaming && ./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: 提交**
```bash
git add src/main/java/org/example/bigevent/config/SecurityConfig.java && git commit -m "feat: 创建 Spring Security 配置类"
```

---

### Task 7: 修改 UserServiceImpl 使用 BCrypt

**Files:**
- Modify: `src/main/java/org/example/bigevent/service/impl/UserServiceImpl.java`

- [ ] **Step 1: 查看并修改 UserServiceImpl**

找到 `register` 方法，将密码加密从 `Md5Util` 改为 `BCrypt`：

```java
@Autowired
private PasswordEncoder passwordEncoder;

@Override
public void register(String username, String password) {
    // 使用 BCrypt 加密
    String encodedPassword = passwordEncoder.encode(password);
    userMapper.add(username, encodedPassword);
}
```

找到 `updatePwd` 方法，同样使用 BCrypt：

```java
@Override
public void updatePwd(String newPwd) {
    Map<String, Object> map = ThreadLocalUtil.get();
    Integer userId = (Integer) map.get("userId");
    String password = passwordEncoder.encode(newPwd);
    userMapper.updatePwd(userId, password);
}
```

修改 `login` 方法，移除手动验证，改为 Spring Security 认证：

```java
// login 方法现在由 Spring Security 处理，这里保留用于兼容
// 如果使用 Spring Security 的认证流程，这个方法可能不再需要
// 保留是因为可能有其他服务需要用到
@Override
public User login(String username, String password) {
    User user = userMapper.findByUsername(username);
    if (user == null) {
        throw new RuntimeException("用户名错误");
    }
    if (!passwordEncoder.matches(password, user.getPassword())) {
        throw new RuntimeException("用户密码错误");
    }
    return user;
}
```

Run: `cd gaming && ./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: 提交**
```bash
git add src/main/java/org/example/bigevent/service/impl/UserServiceImpl.java && git commit -m "feat: UserServiceImpl 集成 BCrypt 加密"
```

---

### Task 8: 修改 UserController 适配 Spring Security

**Files:**
- Modify: `src/main/java/org/example/bigevent/controller/UserController.java`

- [ ] **Step 1: 修改 UserController**

移除 `@Validated`，简化 login 方法（Spring Security 处理认证）：

```java
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    UserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/login")
    public Result login(String username, String password) {
        // Spring Security 进行认证，这里生成 JWT 返回
        try {
            User user = userService.login(username, password);
            Map<String, Object> UserMap = new HashMap<>();
            UserMap.put("userId", user.getId());
            UserMap.put("username", user.getUsername());
            UserMap.put("password", user.getPassword()); // BCrypt 加密后的密码
            String JwtToken = JwtUtil.genToken(UserMap);
            stringRedisTemplate.opsForValue().set("token" + user.getId(), JwtToken, 12, TimeUnit.HOURS);
            return Result.success(JwtToken);
        } catch (Exception e) {
            return Result.error("用户名或密码错误");
        }
    }

    @PostMapping("/register")
    public Result register(String username, String password) {
        // 验证逻辑由 Controller 层的 @Valid 处理
        userService.register(username, password);
        return Result.success();
    }

    // 其他方法保持不变...
}
```

注意：register 方法的参数验证需要添加 `@Valid` 或手动验证，因为移除了类级别的 `@Validated`。

- [ ] **Step 2: 提交**
```bash
git add src/main/java/org/example/bigevent/controller/UserController.java && git commit -m "feat: UserController 适配 Spring Security 认证"
```

---

### Task 9: 删除 LoginInterceptor 并更新 WebConfig

**Files:**
- Delete: `src/main/java/org/example/bigevent/Interceptors/LoginInterceptor.java`
- Modify: `src/main/java/org/example/bigevent/WebConfig.java`

- [ ] **Step 1: 删除 LoginInterceptor.java**

在文件系统中删除该文件。

- [ ] **Step 2: 简化 WebConfig.java**

```java
package org.example.bigevent;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // Spring Security 处理认证，不再需要拦截器
    // 可以保留为空配置类，或删除此文件
}
```

- [ ] **Step 3: 提交**
```bash
git rm src/main/java/org/example/bigevent/Interceptors/LoginInterceptor.java
git add src/main/java/org/example/bigevent/WebConfig.java
git commit -m "refactor: 移除 LoginInterceptor，Spring Security 接管认证"
```

---

### Task 10: 更新数据库表结构

**Files:**
- Modify: `sql/schema.sql`

- [ ] **Step 1: 在 schema.sql 添加新表和修改**

```sql
-- 修改 user 表，添加 role 字段
ALTER TABLE `user` ADD COLUMN `role` varchar(50) DEFAULT 'ROLE_USER' COMMENT '角色';

-- 新建 role 表
CREATE TABLE IF NOT EXISTS `role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_alias` varchar(50) NOT NULL COMMENT '角色标识',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 新建 user_role 表
CREATE TABLE IF NOT EXISTS `user_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '用户ID',
  `role_id` int NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化角色数据
INSERT INTO `role` (`role_name`, `role_alias`) VALUES
  ('普通用户', 'ROLE_USER'),
  ('内容管理员', 'ROLE_CONTENT_ADMIN'),
  ('超级管理员', 'ROLE_SUPER_ADMIN');
```

- [ ] **Step 2: 提交**
```bash
git add sql/schema.sql && git commit -m "feat: 添加角色表和 user_role 关联表"
```

---

## 计划总结

| Task | 文件 | 操作 |
|------|------|------|
| 1 | pom.xml | 添加 Spring Security 依赖 |
| 2 | config/PasswordConfig.java | 创建 BCrypt 配置 |
| 3 | pojo/User.java | 实现 UserDetails 接口 |
| 4 | security/UserDetailsServiceImpl.java | 创建用户认证服务 |
| 5 | security/JwtAuthenticationFilter.java | 创建 JWT 过滤器 |
| 6 | config/SecurityConfig.java | 创建安全配置 |
| 7 | service/impl/UserServiceImpl.java | 集成 BCrypt |
| 8 | controller/UserController.java | 适配 Spring Security |
| 9 | Interceptors/LoginInterceptor.java, WebConfig.java | 删除旧拦截器 |
| 10 | sql/schema.sql | 更新数据库 |

---

**Plan complete and saved to `docs/superpowers/plans/2026-05-11-spring-security-plan.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**