package org.example.bigevent.controller;

import org.example.bigevent.utils.JwtUtil;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.pojo.User;
import org.example.bigevent.service.UserService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    @PostMapping("/register")
    public Result register(@RequestBody Map<String, String> registerData) {
            String username = registerData.get("username");
            String password = registerData.get("password");
            if (!StringUtils.hasLength(username) || !StringUtils.hasLength(password)) {
                return Result.error("用户名或密码不能为空");
            }
            User u = userService.findByUserName(username);
            if (u == null) {
                userService.register(username,password);
                return Result.success();
            }
            else {
                return Result.error("用户已存在");
            }
        }
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
            Map<String,Object> UserMap = new HashMap<>();
            UserMap.put("userId",u.getId());
            UserMap.put("username",u.getUsername());
            UserMap.put("role",u.getRole());

            long expiration = Boolean.TRUE.equals(rememberme) ? REMEMBER_ME_EXPIRATION : DEFAULT_EXPIRATION;
            String JwtToken = jwtUtil.genToken(UserMap, expiration);

            long redisExpireSeconds = expiration / 1000;
            stringRedisTemplate.opsForValue().set("token"+u.getId(),JwtToken,redisExpireSeconds, TimeUnit.SECONDS);

            return Result.success(JwtToken);
        }
        return Result.error("用户密码错误");
    }

    @GetMapping("/userInfo")
    public Result<User> userInfo(){
        Map<String, Object> tmp = ThreadLocalUtil.get();
        String username = (String)tmp.get("username");
        User u = userService.findByUserName(username);
        return Result.success(u);
    }

    @GetMapping("/list")
    public Result<List<User>> list(){
        List<User> users = userService.list();
        return Result.success(users);
    }

    @PutMapping("/update")
    public Result update(@RequestBody @Validated User user){
        userService.update(user);
        return Result.success();

    }

    @PatchMapping("/updateAvatar")
    public Result updateAvatar(@RequestParam @URL String avatarUrl){
        userService.updateAvater(avatarUrl);
        return Result.success();
    }

    @PatchMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String,String> params){
        String oldPwd = params.get("old_pwd");
        String newPwd = params.get("new_pwd");
        String rePwd = params.get("re_pwd");
        if (!StringUtils.hasLength(oldPwd) || !StringUtils.hasLength(newPwd) ||! StringUtils.hasLength(rePwd)) {
            return Result.error("密码不能为空");
        }
        Map<String,Object> map = ThreadLocalUtil.get();
        String username = (String)map.get("username");
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
}
