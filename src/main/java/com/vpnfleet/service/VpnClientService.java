package com.vpnfleet.service;

import com.vpnfleet.dto.ClientResponse;
import com.vpnfleet.entity.ClientStatus;
import com.vpnfleet.entity.VpnClient;
import com.vpnfleet.entity.VpnNode;
import com.vpnfleet.exception.ApiException;
import com.vpnfleet.mapper.VpnClientMapper;
import com.vpnfleet.mapper.VpnNodeMapper;
import com.vpnfleet.provisioning.VpnProvisioner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates VPN client issuance/revocation: picks a candidate node, takes
 * a per-node Redisson distributed lock so concurrent requests can't
 * over-allocate a capacity-limited node, then delegates the actual DB work
 * to {@link VpnClientProvisioningTx} (a separate bean, so the
 * {@code @Transactional} there goes through a real Spring proxy).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VpnClientService {

    private static final String LOCK_KEY_PREFIX = "vpnfleet:node-lock:";
    private static final long LOCK_WAIT_SECONDS = 5;
    private static final long LOCK_LEASE_SECONDS = 10;

    private final VpnClientMapper vpnClientMapper;
    private final VpnNodeMapper vpnNodeMapper;
    private final VpnClientProvisioningTx provisioningTx;
    private final VpnProvisioner vpnProvisioner;
    private final RedissonClient redissonClient;
    private final CacheManager cacheManager;

    public List<ClientResponse> listForUser(Long userId) {
        return vpnClientMapper.findByUserId(userId).stream().map(ClientResponse::from).toList();
    }

    public List<ClientResponse> listAll() {
        return vpnClientMapper.findAll().stream().map(ClientResponse::from).toList();
    }

    /**
     * Issues a new client, either on the requested node or, if none was
     * requested, on whichever currently-active node has the most free
     * capacity. Each candidate node is attempted under its own distributed
     * lock so two concurrent callers can never both succeed against the same
     * node once it's full.
     */
    public ClientResponse issueClient(Long userId, Long requestedNodeId) {
        List<Long> candidateNodeIds = requestedNodeId != null
                ? List.of(requestedNodeId)
                : autoPickCandidateNodeIds();

        if (candidateNodeIds.isEmpty()) {
            throw ApiException.conflict("No VPN nodes with available capacity right now");
        }

        ApiException lastFailure = null;
        for (Long nodeId : candidateNodeIds) {
            try {
                return issueOnNode(userId, nodeId);
            } catch (ApiException e) {
                lastFailure = e;
                log.info("Node {} unavailable for issuance ({}), trying next candidate", nodeId, e.getMessage());
            }
        }
        throw lastFailure != null ? lastFailure : ApiException.conflict("No VPN nodes with available capacity right now");
    }

    private List<Long> autoPickCandidateNodeIds() {
        return vpnNodeMapper.findAll().stream()
                .filter(VpnNode::hasFreeCapacity)
                .sorted(Comparator.comparingInt(n -> n.getActiveClientCount() - n.getCapacity()))
                .map(VpnNode::getId)
                .toList();
    }

    private ClientResponse issueOnNode(Long userId, Long nodeId) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + nodeId);
        boolean locked;
        try {
            locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ApiException.conflict("Interrupted while allocating a VPN node");
        }
        if (!locked) {
            throw ApiException.conflict("Node " + nodeId + " is busy, please retry");
        }

        try {
            ClientResponse response = provisioningTx.issueOnLockedNode(userId, nodeId);
            evictNodesCache();
            return response;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public ClientResponse revokeClient(Long requestingUserId, boolean isAdmin, Long clientId) {
        VpnClient client = vpnClientMapper.findById(clientId)
                .orElseThrow(() -> ApiException.notFound("No client with id " + clientId));

        if (!isAdmin && !client.getUserId().equals(requestingUserId)) {
            throw ApiException.forbidden("You do not own this VPN client");
        }
        if (client.getStatus() == ClientStatus.REVOKED) {
            throw ApiException.conflict("Client is already revoked");
        }

        VpnNode node = vpnNodeMapper.findById(client.getNodeId())
                .orElseThrow(() -> ApiException.notFound("No node with id " + client.getNodeId()));

        vpnProvisioner.revoke(node, client.getExternalClientId());
        vpnClientMapper.revoke(clientId);
        vpnNodeMapper.decrementActiveClientCount(client.getNodeId());
        evictNodesCache();

        client.setStatus(ClientStatus.REVOKED);
        client.setRevokedAt(OffsetDateTime.now());
        return ClientResponse.from(client);
    }

    private void evictNodesCache() {
        var cache = cacheManager.getCache("nodes");
        if (cache != null) {
            cache.evict("all");
        }
    }
}
