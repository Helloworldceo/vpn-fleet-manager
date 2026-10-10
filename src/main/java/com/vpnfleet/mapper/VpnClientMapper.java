package com.vpnfleet.mapper;

import com.vpnfleet.entity.VpnClient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface VpnClientMapper {
    List<VpnClient> findByUserId(@Param("userId") Long userId);

    Optional<VpnClient> findById(@Param("id") Long id);

    List<VpnClient> findAll();

    void insert(VpnClient client);

    void revoke(@Param("id") Long id);
}
