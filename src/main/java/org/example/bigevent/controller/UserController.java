package org.example.bigevent.controller;

import ch.qos.logback.core.util.StringUtil;
import jakarta.validation.constraints.Pattern;
import org.example.bigevent.utils.JwtUtil;
import org.example.bigevent.pojo.Result;
import org.example.bigevent.pojo.User;
import org.example.bigevent.service.UserService;
import org.example.bigevent.utils.Md5Util;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Validated
public class UserController {
    @Autowired
    UserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @PostMapping("/register")
    public Result register(@Pattern(regexp = "^//S{5,16}$") String username,
                           @Pattern(regexp = "^//S{5,16}$")String password) {
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
    public Result login(@Pattern(regexp = "^//S{5,16}$") String username,
                        @Pattern(regexp = "^//S{5,16}$")String password) {
        User u = userService.findByUserName(username);
        if (u == null) {
            return Result.error("用户名错误");
        }
        if (Md5Util.getMD5String(password).equals(u.getPassword())) {
            Map<String,Object> UserMap = new HashMap<>();
            UserMap.put("userId",u.getId());
            UserMap.put("username",u.getUsername());
            String JwtToken = JwtUtil.genToken(UserMap);
            stringRedisTemplate.opsForValue().set("token"+u.getId(),JwtToken,12, TimeUnit.HOURS);

            return Result.success(JwtToken);
        }
        return Result.error("用户密码错误");
    }

    @GetMapping("/userInfo")
    public Result<User> userInfo(){
        //这里是为了从token中找到用户名从而找到用户信息返回
        Map<String, Object> tmp = ThreadLocalUtil.get();
        String username = (String)tmp.get("username");
        User u = userService.findByUserName(username);
        return Result.success(u);
    }

    @PutMapping("/update")
    //@RequestBody的作用是将请求体中的json字符串转换为User对象
    public Result update(@RequestBody @Validated User user){
        userService.update(user);
        return Result.success();

    }

    @PatchMapping("/updateAvatar")
    //@RequestParam的作用是将请求参数中的avatarUrl转换为String类型
    public Result updateAvatar(@RequestParam @URL String avatarUrl){
        userService.updateAvater(avatarUrl);
        return Result.success();
    }

    @PatchMapping("/updatePwd")
    //@RequestBody的作用是将请求体中的json字符串转换为User对象
    public Result updatePwd(@RequestBody Map<String,String> params){
        String oldPwd = params.get("old_pwd");
        String newPwd = params.get("new_pwd");
        String rePwd = params.get("re_pwd");
        //参数校验
        //判断密码是否为空
        if (!StringUtils.hasLength(oldPwd) || ! StringUtils.hasLength(newPwd) ||! StringUtils.hasLength(rePwd)) {
            return Result.error("密码不能为空");
        }
        //判断密码是否一致
        Map<String,Object> map = ThreadLocalUtil.get();
        String username = (String)map.get("username");
        String password = userService.findByUserName(username).getPassword();
        if (!Md5Util.getMD5String(oldPwd).equals(password)) {
            return Result.error("旧密码错误");
        }
        //判断新密码是否一致
        if (!newPwd.equals(rePwd)) {
            return Result.error("两次密码不一致");
        }
        userService.updatePwd(newPwd);
        return Result.success();

    }
}

