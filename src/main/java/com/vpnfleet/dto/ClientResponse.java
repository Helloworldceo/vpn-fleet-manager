package com.vpnfleet.dto;

import com.vpnfleet.entity.VpnClient;

import java.time.OffsetDateTime;

public record ClientResponse(
        Long id,
        Long nodeId,
        String config,
        String status,
        OffsetDateTime issuedAt,
        OffsetDateTime revokedAt,
        OffsetDateTime expiresAt
) {
    public static ClientResponse from(VpnClient client) {
        return new ClientResponse(
                client.getId(),
                client.getNodeId(),
                client.getConfig(),
                client.getStatus().name(),
                client.getIssuedAt(),
                client.getRevokedAt(),
                client.getExpiresAt()
        );
    }
}
