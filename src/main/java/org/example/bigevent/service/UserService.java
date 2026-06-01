package org.example.bigevent.service;

import org.example.bigevent.pojo.User;
import org.hibernate.validator.constraints.URL;

import java.util.List;

public interface UserService {
    //根据用户名查询用户
    User findByUserName(String username);
    //注册用户
    void register(String username, String password);

    void update(User user);

    void updateAvater(@URL String avatarUrl);

    void updatePwd(String newPwd);

    List<User> list();
}
