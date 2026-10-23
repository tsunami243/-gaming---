package org.example.bigevent.service.impl;

import org.example.bigevent.mapper.UserMapper;
import org.example.bigevent.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security 用户加载服务：登录认证时按用户名取用户。
 *
 * <p>{@link #updatePassword} 为接口要求实现的方法，这里刻意不做持久化——
 * 口令修改走业务接口，避免认证框架隐式改库。</p>
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService, UserDetailsPasswordService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 按用户名加载用户（含加密口令与角色，供框架比对）。
     *
     * @param username 登录用户名
     * @return 用户实体（本身实现了 {@link UserDetails}）
     * @throws UsernameNotFoundException 用户不存在时抛出，由框架转换为认证失败
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户名不存在");
        }
        return user;
    }

    /**
     * 口令升级回调：本实现不落库，原样返回用户，避免框架隐式修改数据。
     *
     * @param user        当前用户
     * @param newPassword 新口令
     * @return 原样返回的 {@code user}
     */
    @Override
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        return user;
    }
}
