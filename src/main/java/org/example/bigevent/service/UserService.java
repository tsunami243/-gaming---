package org.example.bigevent.service;

import org.example.bigevent.pojo.User;
import org.hibernate.validator.constraints.URL;

import java.util.List;

/** 用户业务：注册、查询、资料与口令更新。 */
public interface UserService {

    /**
     * 根据用户名查询用户。
     *
     * @param username 用户名
     * @return 用户实体，不存在时返回 {@code null}
     */
    User findByUserName(String username);

    /**
     * 注册用户（口令以 BCrypt 加密后落库）。
     *
     * @param username 用户名
     * @param password 明文口令
     */
    void register(String username, String password);

    /**
     * 更新当前登录用户的资料。
     *
     * @param user 用户实体（以当前登录态确定更新目标）
     */
    void update(User user);

    /**
     * 更新当前登录用户的头像地址。
     *
     * @param avatarUrl 头像 URL
     */
    void updateAvater(@URL String avatarUrl);

    /**
     * 修改当前登录用户的口令。
     *
     * @param newPwd 新口令明文
     */
    void updatePwd(String newPwd);

    /**
     * 查询用户列表（需超级管理员权限）。
     *
     * @return 用户列表
     */
    List<User> list();
}
