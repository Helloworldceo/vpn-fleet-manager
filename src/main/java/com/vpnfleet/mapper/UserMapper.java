package com.vpnfleet.mapper;

import com.vpnfleet.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper {
    Optional<User> findByEmail(@Param("email") String email);

    Optional<User> findById(@Param("id") Long id);

    void insert(User user);
}
