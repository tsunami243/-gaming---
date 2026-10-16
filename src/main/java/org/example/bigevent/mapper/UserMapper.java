package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.bigevent.pojo.User;

import java.util.List;

/**
 * 用户数据访问。
 *
 * <p>注意 {@link #findByuserName(String)} 是 {@link #findByUsername(String)} 的
 * 历史重复实现（方法名大小写不同），保留以兼容既有调用方。</p>
 */
@Mapper
public interface UserMapper {

    /**
     * 按用户名查询用户（认证入口）。
     *
     * @param username 用户名
     * @return 用户实体，不存在时返回 {@code null}
     */
    @Select("SELECT * FROM `user` WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    /**
     * 按用户名查询用户（历史重复实现，方法名大小写与上者不同）。
     *
     * @param username 用户名
     * @return 用户实体，不存在时返回 {@code null}
     */
    @Select("SELECT * FROM `user` WHERE username = #{username}")
    User findByuserName(String username);

    /**
     * 新增用户（口令已加密，时间戳由数据库生成）。
     *
     * @param username  用户名
     * @param md5String 加密后的口令
     */
    @Insert("INSERT INTO `user`(username,`password`,create_time,update_time)" +
    " VALUES (#{username},#{md5String},now(),now())")
    void add(String username, String md5String);

    /**
     * 更新用户的昵称与邮箱。
     *
     * @param user 用户实体，需含 id、nickname、email 与 updateTime
     */
    @Update("UPDATE user SET nickname = #{nickname}, email =#{email}, update_time = #{updateTime} where id = #{id}")
    void update(User user);

    /**
     * 更新用户头像地址。
     *
     * @param avatarUrl 头像 URL
     * @param id        用户 ID
     */
    @Update("update user set user_pic = #{avatarUrl},update_time = now() where id = #{id}")
    void updateAvatar(String avatarUrl, Integer id);

    /**
     * 更新用户口令。
     *
     * @param md5String 加密后的新口令
     * @param id        用户 ID
     */
    @Update("update user set password = #{md5String},update_time = now() where id = #{id}")
    void updatePwd(String md5String, Integer id);

    /**
     * 查询全部用户（需超级管理员权限）。
     *
     * @return 用户列表
     */
    @Select("SELECT * FROM `user`")
    List<User> list();
}
