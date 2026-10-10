package com.vpnfleet.provisioning;

import com.vpnfleet.entity.VpnNode;

/**
 * Boundary between this service and whatever actually turns a node + protocol
 * into a working VPN client. {@link MockVpnProvisioner} is the only
 * implementation wired up today — it never makes a network call, which keeps
 * this portfolio project safe to run and publish without touching any real
 * infrastructure. A real implementation (e.g. one that calls a 3x-ui-style
 * panel API over SSH/HTTPS on an actual node) could implement this same
 * interface and be swapped in via Spring configuration without any change to
 * {@code VpnClientService} or the controllers above it.
 */
public interface VpnProvisioner {
    ProvisionedConfig provision(VpnNode node, Long userId);

    void revoke(VpnNode node, String externalClientId);
}
