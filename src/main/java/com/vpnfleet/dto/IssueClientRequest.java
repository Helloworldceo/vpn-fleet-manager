package com.vpnfleet.dto;

/** {@code nodeId} is optional — omit it to let the service auto-pick any node with free capacity. */
public record IssueClientRequest(Long nodeId) {
}
