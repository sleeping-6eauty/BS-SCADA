package com.example.backend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.backend.domain.User;

@Mapper
public interface UserMapper {
    User findByEmail(@Param("email") String email);
    User findById(@Param("id") Long id);
    List<User> findAll();
    int insert(User user);
    int update(User user);
    int deleteById(@Param("id") Long id);
}
