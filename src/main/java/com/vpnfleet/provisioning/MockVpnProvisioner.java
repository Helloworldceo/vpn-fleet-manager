package com.vpnfleet.provisioning;

import com.vpnfleet.entity.VpnNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Default {@link VpnProvisioner}. Generates a realistic-looking VLESS+REALITY
 * client config entirely in-process, with a small simulated latency so the
 * API behaves like it's really talking to a remote panel. Makes no network
 * calls and stores no credentials to any real server.
 */
@Slf4j
@Component
public class MockVpnProvisioner implements VpnProvisioner {

    private static final String[] CAMOUFLAGE_SNI = {
            "www.microsoft.com", "dl.google.com", "addons.mozilla.org", "swcdn.apple.com"
    };
    private final SecureRandom random = new SecureRandom();

    @Override
    public ProvisionedConfig provision(VpnNode node, Long userId) {
        simulateLatency();

        String clientUuid = UUID.randomUUID().toString();
        String shortId = randomHex(8);
        String publicKey = randomBase64Url(32);
        String sni = CAMOUFLAGE_SNI[random.nextInt(CAMOUFLAGE_SNI.length)];
        String label = "vpn-fleet-" + node.getName() + "-u" + userId;

        String config = "vless://" + clientUuid + "@" + node.getHost() + ":443"
                + "?encryption=none&flow=xtls-rprx-vision&security=reality"
                + "&sni=" + sni + "&fp=chrome&pbk=" + publicKey + "&sid=" + shortId
                + "&type=tcp#" + label;

        log.info("Provisioned mock client {} on node {}", clientUuid, node.getName());
        return new ProvisionedConfig(clientUuid, config);
    }

    @Override
    public void revoke(VpnNode node, String externalClientId) {
        simulateLatency();
        log.info("Revoked mock client {} on node {}", externalClientId, node.getName());
    }

    private void simulateLatency() {
        try {
            Thread.sleep(20 + random.nextInt(60));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String randomHex(int bytes) {
        byte[] buf = new byte[bytes];
        random.nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String randomBase64Url(int bytes) {
        byte[] buf = new byte[bytes];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
