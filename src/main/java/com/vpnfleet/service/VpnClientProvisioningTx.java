package com.vpnfleet.service;

import com.vpnfleet.dto.ClientResponse;
import com.vpnfleet.entity.ClientStatus;
import com.vpnfleet.entity.VpnClient;
import com.vpnfleet.entity.VpnNode;
import com.vpnfleet.exception.ApiException;
import com.vpnfleet.mapper.VpnClientMapper;
import com.vpnfleet.mapper.VpnNodeMapper;
import com.vpnfleet.provisioning.ProvisionedConfig;
import com.vpnfleet.provisioning.VpnProvisioner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * The actual insert/increment, run as its own Spring-managed bean so
 * {@code @Transactional} is applied through a real proxy. {@link VpnClientService}
 * holds the Redisson lock and calls into this bean rather than a method on
 * itself — a same-class ("self-invocation") call would silently bypass
 * Spring's transactional proxy entirely.
 */
@Service
@RequiredArgsConstructor
public class VpnClientProvisioningTx {

    private final VpnClientMapper vpnClientMapper;
    private final VpnNodeMapper vpnNodeMapper;
    private final VpnProvisioner vpnProvisioner;

    @Transactional
    public ClientResponse issueOnLockedNode(Long userId, Long nodeId) {
        VpnNode node = vpnNodeMapper.findById(nodeId)
                .orElseThrow(() -> ApiException.notFound("No node with id " + nodeId));

        if (!node.hasFreeCapacity()) {
            throw ApiException.conflict("Node " + node.getName() + " is at full capacity");
        }

        ProvisionedConfig provisioned = vpnProvisioner.provision(node, userId);

        VpnClient client = VpnClient.builder()
                .userId(userId)
                .nodeId(nodeId)
                .externalClientId(provisioned.externalClientId())
                .config(provisioned.config())
                .status(ClientStatus.ACTIVE)
                .issuedAt(OffsetDateTime.now())
                .build();
        vpnClientMapper.insert(client);

        int affected = vpnNodeMapper.incrementActiveClientCountIfCapacityAvailable(nodeId);
        if (affected == 0) {
            // Defensive: should be unreachable under the caller's distributed lock,
            // but never leave an orphaned client provisioned against a full node.
            vpnClientMapper.revoke(client.getId());
            vpnProvisioner.revoke(node, provisioned.externalClientId());
            throw ApiException.conflict("Node " + node.getName() + " filled up during allocation, please retry");
        }

        return ClientResponse.from(client);
    }
}
