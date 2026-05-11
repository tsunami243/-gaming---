package org.example.bigevent.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.bigevent.pojo.User;
@Mapper
public interface UserMapper {
    @Select("SELECT * FROM `user` WHERE username = #{username}")
User findByUsername(@Param("username") String username);
    @Select("SELECT * FROM `user` WHERE username = #{username}")
    User findByuserName(String username);
    @Insert("INSERT INTO `user`(username,`password`,create_time,update_time)"+
    " VALUES (#{username},#{md5String},now(),now())")
    void add(String username, String md5String);
    @Update("UPDATE user SET nickname = #{nickname}, email =#{email}, update_time = #{updateTime} where id = #{id}")
    void update(User user);
    @Update("update user set user_pic = #{avatarUrl},update_time = now() where id = #{id}")
    void updateAvatar(String avatarUrl, Integer id);
    @Update("update user set password = #{md5String},update_time = now() where id = #{id}")
    void updatePwd(String md5String, Integer id);
}
