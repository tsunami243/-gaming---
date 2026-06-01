package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.UserMapper;
import org.example.bigevent.pojo.User;
import org.example.bigevent.service.UserService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserMapper userMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    @Override
    public User findByUserName(String username) {
        User u = userMapper.findByuserName(username);
        return u;
    }

    @Override
    public void register(String username, String password) {
        //使用 BCrypt 加密
        String encodedPassword = passwordEncoder.encode(password);
        userMapper.add(username, encodedPassword);
    }

    @Override
    public void update(User user) {
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
    }

    @Override
    public void updateAvater(String avatarUrl) {
        Map<String, Object> tmp = ThreadLocalUtil.get();
        Integer id = (Integer) tmp.get("userId");
        userMapper.updateAvatar(avatarUrl,id);
    }

    @Override
    public void updatePwd(String newPwd) {
        Map<String, Object> tmp = ThreadLocalUtil.get();
        Integer id = (Integer) tmp.get("userId");
        String password = passwordEncoder.encode(newPwd);
        userMapper.updatePwd(password, id);
        //更新密码后立马删除redis中存入的token
        stringRedisTemplate.opsForValue().getOperations().delete("token" + id);
    }

    @Override
    public List<User> list() {
        return userMapper.list();
    }
}
