package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.UserMapper;
import org.example.bigevent.pojo.User;
import org.example.bigevent.service.UserService;
import org.example.bigevent.utils.Md5Util;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserMapper userMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    public User findByUserName(String username) {
        User u = userMapper.findByuserName(username);
        return u;
    }

    @Override
    public void register(String username, String password) {
        //先加密
        String md5String = Md5Util.getMD5String(password);
        //添加
        userMapper.add(username,md5String);
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
        userMapper.updatePwd(Md5Util.getMD5String(newPwd),id);
        //更新密码后立马删除redis中存入的token
        stringRedisTemplate.opsForValue().getOperations().delete("token" + id);
    }
}
