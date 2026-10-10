package com.vpnfleet.dto;

import com.vpnfleet.entity.VpnNode;

public record NodeResponse(
        Long id,
        String name,
        String region,
        String protocol,
        int capacity,
        int activeClientCount,
        int freeSlots,
        String status
) {
    public static NodeResponse from(VpnNode node) {
        return new NodeResponse(
                node.getId(),
                node.getName(),
                node.getRegion(),
                node.getProtocol(),
                node.getCapacity(),
                node.getActiveClientCount(),
                Math.max(0, node.getCapacity() - node.getActiveClientCount()),
                node.getStatus().name()
        );
    }
}
