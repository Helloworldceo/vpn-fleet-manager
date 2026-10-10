package com.vpnfleet.provisioning;

import com.vpnfleet.entity.NodeStatus;
import com.vpnfleet.entity.VpnNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MockVpnProvisionerTest {

    private final MockVpnProvisioner provisioner = new MockVpnProvisioner();

    @Test
    void provisionReturnsAWellFormedVlessConfig() {
        VpnNode node = VpnNode.builder()
                .id(1L).name("test-node").host("vpn.example.com")
                .region("eu").protocol("VLESS_REALITY")
                .capacity(10).activeClientCount(0).status(NodeStatus.ACTIVE)
                .build();

        ProvisionedConfig result = provisioner.provision(node, 42L);

        assertThat(result.externalClientId()).isNotBlank();
        assertThat(result.config())
                .startsWith("vless://")
                .contains(result.externalClientId())
                .contains("vpn.example.com:443")
                .contains("security=reality");
    }

    @Test
    void eachProvisionCallProducesAUniqueClient() {
        VpnNode node = VpnNode.builder()
                .id(1L).name("test-node").host("vpn.example.com")
                .region("eu").protocol("VLESS_REALITY")
                .capacity(10).activeClientCount(0).status(NodeStatus.ACTIVE)
                .build();

        ProvisionedConfig first = provisioner.provision(node, 1L);
        ProvisionedConfig second = provisioner.provision(node, 1L);

        assertThat(first.externalClientId()).isNotEqualTo(second.externalClientId());
    }

    @Test
    void revokeDoesNotThrow() {
        VpnNode node = VpnNode.builder().id(1L).name("test-node").host("vpn.example.com").build();
        assertThatCode(() -> provisioner.revoke(node, "some-client-id")).doesNotThrowAnyException();
    }
}
