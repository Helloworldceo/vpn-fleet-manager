package com.vpnfleet.service;

import com.vpnfleet.dto.ClientResponse;
import com.vpnfleet.entity.ClientStatus;
import com.vpnfleet.entity.NodeStatus;
import com.vpnfleet.entity.VpnClient;
import com.vpnfleet.entity.VpnNode;
import com.vpnfleet.exception.ApiException;
import com.vpnfleet.mapper.VpnClientMapper;
import com.vpnfleet.mapper.VpnNodeMapper;
import com.vpnfleet.provisioning.VpnProvisioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VpnClientServiceTest {

    @Mock private VpnClientMapper vpnClientMapper;
    @Mock private VpnNodeMapper vpnNodeMapper;
    @Mock private VpnClientProvisioningTx provisioningTx;
    @Mock private VpnProvisioner vpnProvisioner;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock rLock;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;

    private VpnClientService service;

    @BeforeEach
    void setUp() {
        service = new VpnClientService(vpnClientMapper, vpnNodeMapper, provisioningTx, vpnProvisioner, redissonClient, cacheManager);
    }

    @Test
    void issueClient_withExplicitNode_locksProvisionsAndEvictsCache() throws InterruptedException {
        when(redissonClient.getLock("vpnfleet:node-lock:5")).thenReturn(rLock);
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        ClientResponse expected = new ClientResponse(1L, 5L, "vless://abc", "ACTIVE", OffsetDateTime.now(), null, null);
        when(provisioningTx.issueOnLockedNode(99L, 5L)).thenReturn(expected);
        when(cacheManager.getCache("nodes")).thenReturn(cache);

        ClientResponse result = service.issueClient(99L, 5L);

        assertThat(result).isEqualTo(expected);
        verify(rLock).unlock();
        verify(cache).evict("all");
    }

    @Test
    void issueClient_whenNodeIsBusy_throwsConflict() throws InterruptedException {
        when(redissonClient.getLock("vpnfleet:node-lock:5")).thenReturn(rLock);
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(false);

        assertThatThrownBy(() -> service.issueClient(99L, 5L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("busy");

        verify(provisioningTx, never()).issueOnLockedNode(anyLong(), anyLong());
    }

    @Test
    void issueClient_autoPick_skipsFullNodesAndTriesTheNextCandidate() throws InterruptedException {
        VpnNode full = VpnNode.builder().id(1L).name("full").capacity(5).activeClientCount(5).status(NodeStatus.ACTIVE).build();
        VpnNode open = VpnNode.builder().id(2L).name("open").capacity(5).activeClientCount(1).status(NodeStatus.ACTIVE).build();
        when(vpnNodeMapper.findAll()).thenReturn(java.util.List.of(full, open));

        when(redissonClient.getLock("vpnfleet:node-lock:2")).thenReturn(rLock);
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        ClientResponse expected = new ClientResponse(10L, 2L, "vless://xyz", "ACTIVE", OffsetDateTime.now(), null, null);
        when(provisioningTx.issueOnLockedNode(99L, 2L)).thenReturn(expected);
        when(cacheManager.getCache("nodes")).thenReturn(cache);

        ClientResponse result = service.issueClient(99L, null);

        assertThat(result.nodeId()).isEqualTo(2L);
        verify(redissonClient, never()).getLock("vpnfleet:node-lock:1");
    }

    @Test
    void revokeClient_byNonOwnerNonAdmin_throwsForbidden() {
        VpnClient client = VpnClient.builder().id(1L).userId(7L).nodeId(2L).status(ClientStatus.ACTIVE).build();
        when(vpnClientMapper.findById(1L)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.revokeClient(8L, false, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("do not own");

        verify(vpnProvisioner, never()).revoke(any(), any());
    }

    @Test
    void revokeClient_alreadyRevoked_throwsConflict() {
        VpnClient client = VpnClient.builder().id(1L).userId(7L).nodeId(2L).status(ClientStatus.REVOKED).build();
        when(vpnClientMapper.findById(1L)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.revokeClient(7L, false, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already revoked");
    }
}
