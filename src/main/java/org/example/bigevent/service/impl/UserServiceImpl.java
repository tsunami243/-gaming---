package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.UserMapper;
import org.example.bigevent.pojo.User;
import org.example.bigevent.service.UserService;
import org.example.bigevent.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户账号的业务实现。
 *
 * <p>密码一律以 BCrypt 密文存储，接口不接受明文落库。修改资料时以当前登录态
 * （{@link ThreadLocalUtil} 中的 {@code userId}）为唯一可信身份，忽略请求体里的伪造 ID。
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    /**
     * 按用户名查询用户（含密文密码，仅供登录校验使用）。
     *
     * @param username 用户名
     * @return 用户；不存在返回 {@code null}
     */
    @Override
    public User findByUserName(String username) {
        User u = userMapper.findByuserName(username);
        return u;
    }

    /**
     * 注册新用户，密码以 BCrypt 密文入库。
     *
     * @param username 用户名
     * @param password 明文密码
     */
    @Override
    public void register(String username, String password) {
        //使用 BCrypt 加密
        String encodedPassword = passwordEncoder.encode(password);
        userMapper.add(username, encodedPassword);
    }

    /**
     * 更新本人资料。
     *
     * <p>身份以登录态为准：未登录返回 401，请求体 ID 与登录用户不一致返回 403。
     *
     * @param user 待更新资料，其 ID 必须等于当前登录用户
     * @throws ResponseStatusException 未登录 401；尝试修改他人资料 403
     */
    @Override
    public void update(User user) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null || !(claims.get("userId") instanceof Integer actorId) || actorId < 1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请重新登录");
        }
        if (user == null || !actorId.equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能修改本人资料");
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
    }

    /**
     * 更新当前登录用户的头像。
     *
     * @param avatarUrl 头像地址
     */
    @Override
    public void updateAvater(String avatarUrl) {
        Map<String, Object> tmp = ThreadLocalUtil.get();
        Integer id = (Integer) tmp.get("userId");
        userMapper.updateAvatar(avatarUrl, id);
    }

    /**
     * 修改密码，并立即失效该用户已签发的令牌。
     *
     * <p>密码落库后会删除 Redis 中的 {@code token{userId}}，使旧 JWT 因双验证失败而失效，
     * 从而强制其它端重新登录。
     *
     * @param newPwd 新明文密码
     */
    @Override
    public void updatePwd(String newPwd) {
        Map<String, Object> tmp = ThreadLocalUtil.get();
        Integer id = (Integer) tmp.get("userId");
        String password = passwordEncoder.encode(newPwd);
        userMapper.updatePwd(password, id);
        //更新密码后立马删除redis中存入的token
        stringRedisTemplate.opsForValue().getOperations().delete("token" + id);
    }

    /**
     * 查询全部用户（仅超级管理员可访问该接口）。
     *
     * @return 用户列表
     */
    @Override
    public List<User> list() {
        return userMapper.list();
    }
}
