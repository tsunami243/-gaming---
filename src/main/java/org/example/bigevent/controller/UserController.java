package org.example.bigevent.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.pojo.User;
import org.example.bigevent.service.UserService;
import org.example.bigevent.utils.ClientIpUtil;
import org.example.bigevent.utils.JwtUtil;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.example.bigevent.utils.TokenBucketRateLimiter;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 用户（legacy 接口）。
 *
 * <p>基础路径 {@code /user}；{@code /register}、{@code /login} 公开，其余需登录。
 * 登录成功签发 JWT 并写入 Redis（key = {@code token{userId}}），实现 JWT + Redis 双验证；
 * 改密码与退出登录都删除同一个键，使已签发的令牌立即失效，无需等它自然过期。
 * {@code /list} 仅 SUPER_ADMIN 可访问（见 SecurityConfig）。</p>
 *
 * <p>安全约定：注册与登录都过令牌桶限流；登录失败一律返回同一句提示，
 * 且对不存在的用户也执行一次 BCrypt 比对，避免通过响应内容或耗时枚举有效账号。
 * 限流同时按「账号」与「来源 IP」两个维度计，只按账号会被轮换用户名绕过；
 * 入参先做长度校验，避免用超长用户名把 Redis 撑出巨量 key 并放大 BCrypt 开销。</p>
 */
@RestController
@RequestMapping("/user")
@Validated
public class UserController {

    private static final long REMEMBER_ME_EXPIRATION = 1000L * 60 * 60 * 24 * 3;
    private static final long DEFAULT_EXPIRATION = 1000L * 60 * 60 * 12;

    /** 用户名上限，对齐 {@code user.username} 的 {@code VARCHAR(50)}，超长会直接打库报错。 */
    private static final int MAX_USERNAME_LENGTH = 50;
    /** 口令长度下限。 */
    private static final int MIN_PASSWORD_LENGTH = 10;
    /** BCrypt 只取前 72 字节，按 UTF-8 计（中文等多字节字符会更早触顶）。 */
    private static final int MAX_PASSWORD_BYTES = 72;
    /** 口令复杂度：至少各含一个字母与一个数字。 */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");

    /** 登录限流一：单账号 100 次突发、每秒补 5 个令牌。 */
    private static final int LOGIN_BUCKET_CAPACITY = 100;
    private static final int LOGIN_BUCKET_REFILL_PER_SECOND = 5;
    /** 登录限流二：单来源 IP 60 次突发、每秒补 3 个令牌——堵住轮换用户名绕过账号维度限流。 */
    private static final int LOGIN_IP_BUCKET_CAPACITY = 60;
    private static final int LOGIN_IP_BUCKET_REFILL_PER_SECOND = 3;
    /** 注册限流：全局 20 次突发、每秒补 1 个令牌，抑制批量刷号。 */
    private static final String REGISTER_BUCKET = "register";
    private static final int REGISTER_BUCKET_CAPACITY = 20;
    private static final int REGISTER_BUCKET_REFILL_PER_SECOND = 1;

    /** 登录失败的统一提示：账号不存在与口令错误必须完全一致，否则可用于枚举账号。 */
    private static final String LOGIN_FAILED_MESSAGE = "用户名或密码错误";

    /**
     * 固定的合法 BCrypt 哈希（明文为占位值，无实际账号使用）。
     *
     * <p>用户不存在时用它跑一次比对，使"账号不存在"与"口令错误"两条路径的耗时接近，
     * 防止用响应时间差枚举账号。必须是<b>合法</b> BCrypt 串，否则比对会提前返回、起不到作用。</p>
     */
    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Autowired
    UserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private TokenBucketRateLimiter rateLimiter;

    /**
     * 注册（公开）。
     *
     * <p>{@code POST /user/register}
     *
     * <p>校验顺序：字段必填 → 用户名长度（对齐库表 VARCHAR(50)）→ 口令策略
     * → 注册限流 → 落库。限流放在重名检查之前，避免用"用户已存在"无限次探测账号。</p>
     *
     * <p>并发同名注册时两个请求可能都通过前置查询，由数据库唯一键兜住：
     * 捕获 {@link DuplicateKeyException} 后返回与串行路径一致的提示，而不是让兜底处理器返回 500。</p>
     *
     * @param registerData 含 {@code username} 与 {@code password}
     * @return 空数据；字段不合法、触发限流或用户已存在时返回业务错误
     */
    @PostMapping("/register")
    public Result register(@RequestBody Map<String, String> registerData) {
        String username = registerData.get("username");
        String password = registerData.get("password");
        if (!StringUtils.hasLength(username) || username.length() > MAX_USERNAME_LENGTH) {
            return Result.error("用户名长度需在 1~" + MAX_USERNAME_LENGTH + " 字符之间");
        }
        String passwordError = validatePassword(password);
        if (passwordError != null) {
            return Result.error(passwordError);
        }
        if (!rateLimiter.tryAcquire(REGISTER_BUCKET, REGISTER_BUCKET_CAPACITY, REGISTER_BUCKET_REFILL_PER_SECOND)) {
            return Result.error("注册过于频繁，请稍后再试");
        }
        if (userService.findByUserName(username) != null) {
            return Result.error("用户已存在");
        }
        try {
            userService.register(username, password);
        } catch (DuplicateKeyException e) {
            // 竞态窗口内被其它请求抢先插入，语义上等同于"用户已存在"
            return Result.error("用户已存在");
        }
        return Result.success();
    }

    /**
     * 登录（公开）。
     *
     * <p>{@code POST /user/login}
     *
     * <p>防枚举：无论账号不存在还是口令错误，都返回同一句 {@link #LOGIN_FAILED_MESSAGE}；
     * 账号不存在时改用 {@link #DUMMY_BCRYPT_HASH} 跑一次 BCrypt 比对，抹平耗时差异。</p>
     *
     * <p>限流按「账号 + 来源 IP」双维度计；用户名先做长度校验，避免超长用户名
     * 生成巨量 Redis key，也避免为必然不存在的账号白跑 BCrypt。</p>
     *
     * @param loginData   含 {@code username}、{@code password}，可选 {@code rememberme}
     * @param httpRequest 用于取来源 IP（仅用于限流分桶，见 {@link ClientIpUtil}）
     * @return JWT 令牌；限流命中或凭据错误时返回业务错误。rememberme=true 有效期 3 天，否则 12 小时
     */
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> loginData, HttpServletRequest httpRequest) {
        String username = loginData.get("username");
        String password = loginData.getOrDefault("password", "");
        Boolean rememberme = Boolean.parseBoolean(loginData.getOrDefault("rememberme", "false"));

        if (!StringUtils.hasLength(username) || username.length() > MAX_USERNAME_LENGTH) {
            return Result.error(LOGIN_FAILED_MESSAGE);
        }
        String clientIp = ClientIpUtil.resolve(httpRequest);
        if (!rateLimiter.tryAcquire("login:" + username, LOGIN_BUCKET_CAPACITY, LOGIN_BUCKET_REFILL_PER_SECOND)
                || !rateLimiter.tryAcquire("login-ip:" + clientIp, LOGIN_IP_BUCKET_CAPACITY, LOGIN_IP_BUCKET_REFILL_PER_SECOND)) {
            return Result.error("尝试过于频繁，请稍后再试");
        }

        User u = userService.findByUserName(username);
        // 口令为空的账号按"不存在"处理：否则会拿 DUMMY 哈希去比对，
        // 提交与该占位哈希对应的明文就能绕过校验（当前 schema 是 NOT NULL，属防御性写法）。
        String storedPassword = (u == null) ? null : u.getPassword();
        String encodedPassword = StringUtils.hasLength(storedPassword) ? storedPassword : DUMMY_BCRYPT_HASH;
        boolean matched = passwordEncoder.matches(password, encodedPassword);
        if (u == null || !StringUtils.hasLength(storedPassword) || !matched) {
            return Result.error(LOGIN_FAILED_MESSAGE);
        }

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

    /**
     * 校验口令是否满足策略，注册与改密共用，保证两条入口口径一致。
     *
     * @param password 明文口令，可为 {@code null}
     * @return 不满足时返回可直接回给用户的提示；满足时返回 {@code null}
     */
    private static String validatePassword(String password) {
        if (!StringUtils.hasLength(password) || password.length() < MIN_PASSWORD_LENGTH) {
            return "密码长度不能少于 " + MIN_PASSWORD_LENGTH + " 个字符";
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            return "密码过长（UTF-8 编码下不得超过 " + MAX_PASSWORD_BYTES + " 字节）";
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return "密码需同时包含字母和数字";
        }
        return null;
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
     * <p>新口令走与注册完全相同的 {@link #validatePassword(String)}，否则注册的门槛
     * 可以靠"先注册再改密"绕过去。当前登录用户查不到时按 401 处理，
     * 不再对其口令做解引用（原实现会 NPE 后被兜底成 500）。</p>
     *
     * @param params 含 {@code old_pwd}、{@code new_pwd}、{@code re_pwd}
     * @return 空数据；空值/身份失效/旧密码错误/新口令不合规/两次不一致时返回业务错误
     */
    @PatchMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String, String> params) {
        String oldPwd = params.get("old_pwd");
        String newPwd = params.get("new_pwd");
        String rePwd = params.get("re_pwd");
        if (!StringUtils.hasLength(oldPwd) || !StringUtils.hasLength(newPwd) || !StringUtils.hasLength(rePwd)) {
            return Result.error("密码不能为空");
        }
        String newPasswordError = validatePassword(newPwd);
        if (newPasswordError != null) {
            return Result.error(newPasswordError);
        }
        Map<String, Object> map = ThreadLocalUtil.get();
        String username = (map == null) ? null : (String) map.get("username");
        if (!StringUtils.hasLength(username)) {
            return Result.error("登录状态已失效，请重新登录");
        }
        User current = userService.findByUserName(username);
        if (current == null || !StringUtils.hasLength(current.getPassword())) {
            return Result.error("登录状态已失效，请重新登录");
        }
        if (!passwordEncoder.matches(oldPwd, current.getPassword())) {
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

    /**
     * 退出登录。
     *
     * <p>{@code POST /user/logout}；需登录，未显式放行即落入 {@code anyRequest().authenticated()}。
     *
     * <p>仅清本地令牌是不够的：令牌本身在过期前一直可验签通过，所以服务端必须同步作废它。
     * 本接口沿用改密码时的失效机制——删除 Redis 中的 {@code token{userId}}，
     * 使该令牌在下一次请求时因双验证失败而被判定为未认证。删除目标由已验证的 claims
     * 决定，因此只能作废调用者自己的会话。</p>
     *
     * @return 空数据；令牌已失效（未认证）时由安全链路返回 401
     */
    @PostMapping("/logout")
    public Result logout() {
        userService.logout();
        return Result.success();
    }
}
