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
public class VpnClient {
    private Long id;
    private Long userId;
    private Long nodeId;
    private String externalClientId;
    private String config;
    private ClientStatus status;
    private OffsetDateTime issuedAt;
    private OffsetDateTime revokedAt;
    private OffsetDateTime expiresAt;
}
