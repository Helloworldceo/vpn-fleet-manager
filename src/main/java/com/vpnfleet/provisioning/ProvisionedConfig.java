package com.vpnfleet.provisioning;

/**
 * Result of provisioning a client on a VPN node. {@code config} is an opaque,
 * protocol-specific connection string (e.g. a vless:// URI) that the caller
 * stores as-is and hands back to the end user.
 */
public record ProvisionedConfig(String externalClientId, String config) {
}
