package org.example.bigevent.pojo;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

/**
 * 用户实体：账号、展示信息与角色；同时实现 Spring Security 的 UserDetails 以接入鉴权。
 *
 * <p>{@code password} 存储的是加密后的口令，接口返回前必须置空，禁止外泄。</p>
 */
public class User implements UserDetails {
    @NotNull(message = "主键ID不能为空")
    private Integer id;//主键ID
    private String username;//用户名
    @JsonIgnore
    private String password;//密码
    @NotEmpty(message = "昵称不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{1,10}$", message = "昵称只能包含字母、数字和下划线，且长度在1到10之间")
    private String nickname;//昵称
    @Email(message = "邮箱格式错误")
    private String email;//邮箱
    private String userPic;//用户头像地址
    private String role = "ROLE_USER";//角色
    private LocalDateTime createTime;//创建时间
    private LocalDateTime updateTime;//更新时间

    public User() {
    }

    public User(Integer id, String username, String password, String nickname, String email, String userPic, LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.userPic = userPic;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /**
     * 获取
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * 设置
     * @param id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 获取
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取
     * @return nickname
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 设置
     * @param nickname
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 获取
     * @return email
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置
     * @param email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取
     * @return userPic
     */
    public String getUserPic() {
        return userPic;
    }

    /**
     * 设置
     * @param userPic
     */
    public void setUserPic(String userPic) {
        this.userPic = userPic;
    }

    /**
     * 获取
     * @return createTime
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置
     * @param createTime
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取
     * @return updateTime
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置
     * @param updateTime
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String toString() {
        return "User{id = " + id + ", username = " + username + ", password = " + password + ", nickname = " + nickname + ", email = " + email + ", userPic = " + userPic + ", createTime = " + createTime + ", updateTime = " + updateTime + "}";
    }
}
