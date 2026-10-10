package com.vpnfleet.dto;

public record AuthResponse(String token, String tokenType, Long userId, String email, String role) {
    public static AuthResponse of(String token, Long userId, String email, String role) {
        return new AuthResponse(token, "Bearer", userId, email, role);
    }
}
