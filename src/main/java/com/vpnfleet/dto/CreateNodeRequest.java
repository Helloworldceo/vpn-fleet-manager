package com.vpnfleet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateNodeRequest(
        @NotBlank String name,
        @NotBlank String host,
        @NotBlank String region,
        String protocol,
        @Positive int capacity
) {
}
