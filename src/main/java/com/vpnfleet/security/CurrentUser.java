package com.vpnfleet.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}

    public static AuthenticatedUser get() {
        return (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
