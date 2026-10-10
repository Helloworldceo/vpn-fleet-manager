package com.vpnfleet.mapper;

import com.vpnfleet.entity.VpnNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface VpnNodeMapper {
    List<VpnNode> findAll();

    Optional<VpnNode> findById(@Param("id") Long id);

    /** Row-level lock used while holding the Redisson distributed lock, for defense in depth. */
    Optional<VpnNode> findByIdForUpdate(@Param("id") Long id);

    void insert(VpnNode node);

    void updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * Atomically increments the active client count, but only if capacity isn't already full.
     * Returns the number of affected rows (0 or 1) so the caller can detect a lost race.
     */
    int incrementActiveClientCountIfCapacityAvailable(@Param("id") Long id);

    int decrementActiveClientCount(@Param("id") Long id);
}
