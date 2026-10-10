package com.vpnfleet.security;

import com.vpnfleet.entity.Role;

/** Lightweight principal placed in the security context by {@link JwtAuthFilter}. */
public record AuthenticatedUser(Long id, String email, Role role) {
}
