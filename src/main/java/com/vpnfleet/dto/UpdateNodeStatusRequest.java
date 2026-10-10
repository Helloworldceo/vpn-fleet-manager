package com.vpnfleet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateNodeStatusRequest(
        @NotBlank @Pattern(regexp = "ACTIVE|MAINTENANCE") String status
) {
}
