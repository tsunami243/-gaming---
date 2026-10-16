package org.example.bigevent.controller;

import org.example.bigevent.pojo.Result;
import org.example.bigevent.pojo.User;
import org.example.bigevent.service.UserService;
import org.example.bigevent.utils.JwtUtil;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户（legacy 接口）。
 *
 * <p>基础路径 {@code /user}；{@code /register}、{@code /login} 公开，其余需登录。
 * 登录成功签发 JWT 并写入 Redis（key = {@code token{userId}}），实现 JWT + Redis 双验证；
 * {@code /list} 仅 SUPER_ADMIN 可访问（见 SecurityConfig）。
 */
@RestController
@RequestMapping("/user")
@Validated
public class UserController {

    private static final long REMEMBER_ME_EXPIRATION = 1000L * 60 * 60 * 24 * 3;
    private static final long DEFAULT_EXPIRATION = 1000L * 60 * 60 * 12;

    @Autowired
    UserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 注册（公开）。
     *
     * <p>{@code POST /user/register}
     *
     * @param registerData 含 {@code username} 与 {@code password}
     * @return 空数据；用户名或密码为空、或用户已存在时返回业务错误
     */
    @PostMapping("/register")
    public Result register(@RequestBody Map<String, String> registerData) {
        String username = registerData.get("username");
        String password = registerData.get("password");
        if (!StringUtils.hasLength(username) || !StringUtils.hasLength(password)) {
            return Result.error("用户名或密码不能为空");
        }
        User u = userService.findByUserName(username);
        if (u == null) {
            userService.register(username, password);
            return Result.success();
        } else {
            return Result.error("用户已存在");
        }
    }

    /**
     * 登录（公开）。
     *
     * <p>{@code POST /user/login}
     *
     * @param loginData 含 {@code username}、{@code password}，可选 {@code rememberme}
     * @return JWT 令牌；用户名或密码错误时返回业务错误。rememberme=true 有效期 3 天，否则 12 小时
     */
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");
        Boolean rememberme = Boolean.parseBoolean(loginData.getOrDefault("rememberme", "false"));
        User u = userService.findByUserName(username);
        if (u == null) {
            return Result.error("用户名错误");
        }
        if (passwordEncoder.matches(password, u.getPassword())) {
            Map<String, Object> UserMap = new HashMap<>();
            UserMap.put("userId", u.getId());
            UserMap.put("username", u.getUsername());
            UserMap.put("role", u.getRole());

            long expiration = Boolean.TRUE.equals(rememberme) ? REMEMBER_ME_EXPIRATION : DEFAULT_EXPIRATION;
            String JwtToken = jwtUtil.genToken(UserMap, expiration);

            long redisExpireSeconds = expiration / 1000;
            stringRedisTemplate.opsForValue().set("token" + u.getId(), JwtToken, redisExpireSeconds, TimeUnit.SECONDS);

            return Result.success(JwtToken);
        }
        return Result.error("用户密码错误");
    }

    /**
     * 当前登录用户信息。
     *
     * <p>{@code GET /user/userInfo}
     *
     * @return 当前用户（取自 JWT claims 中的 username）
     */
    @GetMapping("/userInfo")
    public Result<User> userInfo() {
        Map<String, Object> tmp = ThreadLocalUtil.get();
        String username = (String) tmp.get("username");
        User u = userService.findByUserName(username);
        return Result.success(u);
    }

    /**
     * 用户列表（仅 SUPER_ADMIN）。
     *
     * <p>{@code GET /user/list}
     *
     * @return 全部用户
     */
    @GetMapping("/list")
    public Result<List<User>> list() {
        List<User> users = userService.list();
        return Result.success(users);
    }

    /**
     * 更新本人资料。
     *
     * <p>{@code PUT /user/update}
     *
     * @param user 资料（服务端强制使用登录身份，不接受改他人）
     * @return 空数据
     */
    @PutMapping("/update")
    public Result update(@RequestBody @Validated User user) {
        userService.update(user);
        return Result.success();
    }

    /**
     * 更新头像。
     *
     * <p>{@code PATCH /user/updateAvatar?avatarUrl=}
     *
     * @param avatarUrl 头像 URL（须为合法 URL）
     * @return 空数据
     */
    @PatchMapping("/updateAvatar")
    public Result updateAvatar(@RequestParam @URL String avatarUrl) {
        userService.updateAvater(avatarUrl);
        return Result.success();
    }

    /**
     * 修改密码。
     *
     * <p>{@code PATCH /user/updatePwd}
     *
     * @param params 含 {@code old_pwd}、{@code new_pwd}、{@code re_pwd}
     * @return 空数据；空值/旧密码错误/两次不一致时返回业务错误
     */
    @PatchMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String, String> params) {
        String oldPwd = params.get("old_pwd");
        String newPwd = params.get("new_pwd");
        String rePwd = params.get("re_pwd");
        if (!StringUtils.hasLength(oldPwd) || !StringUtils.hasLength(newPwd) || !StringUtils.hasLength(rePwd)) {
            return Result.error("密码不能为空");
        }
        Map<String, Object> map = ThreadLocalUtil.get();
        String username = (String) map.get("username");
        String encodedPassword = userService.findByUserName(username).getPassword();
        if (!passwordEncoder.matches(oldPwd, encodedPassword)) {
            return Result.error("旧密码错误");
        }
        if (!newPwd.equals(rePwd)) {
            return Result.error("两次密码不一致");
        }
        userService.updatePwd(newPwd);
        return Result.success();
    }

    /**
     * 刷新登录令牌。
     *
     * <p>{@code POST /user/refresh}；用当前 claims 重新签发并覆盖 Redis 中的 token。
     *
     * @return 新 JWT 令牌
     */
    @PostMapping("/refresh")
    public Result refresh() {
        Map<String, Object> userMap = ThreadLocalUtil.get();
        String userId = userMap.get("userId").toString();
        String RefreshToken = jwtUtil.genToken(userMap, DEFAULT_EXPIRATION);
        stringRedisTemplate.opsForValue().set("token" + userId, RefreshToken, REMEMBER_ME_EXPIRATION / 1000, TimeUnit.SECONDS);
        return Result.success(RefreshToken);
    }
}
