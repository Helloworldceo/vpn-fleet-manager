package com.vpnfleet.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpnNode {
    private Long id;
    private String name;
    private String host;
    private String region;
    private String protocol;
    private int capacity;
    private int activeClientCount;
    private NodeStatus status;
    private OffsetDateTime createdAt;

    public boolean hasFreeCapacity() {
        return status == NodeStatus.ACTIVE && activeClientCount < capacity;
    }
}
