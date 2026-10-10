package com.vpnfleet.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpnfleet.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Without this, Spring Security's default fallback returns a bare 403 for a
 * request with no/invalid credentials at all -- 401 is the correct status for
 * "not authenticated," reserving 403 for "authenticated but not permitted."
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse(
                Instant.now(), HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
                "Authentication required", request.getRequestURI()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
