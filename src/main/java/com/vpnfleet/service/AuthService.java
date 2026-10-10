package com.vpnfleet.service;

import com.vpnfleet.dto.AuthResponse;
import com.vpnfleet.dto.LoginRequest;
import com.vpnfleet.dto.RegisterRequest;
import com.vpnfleet.entity.Role;
import com.vpnfleet.entity.User;
import com.vpnfleet.exception.ApiException;
import com.vpnfleet.mapper.UserMapper;
import com.vpnfleet.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        userMapper.findByEmail(request.email()).ifPresent(u -> {
            throw ApiException.conflict("An account with this email already exists");
        });

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();
        userMapper.insert(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return AuthResponse.of(token, user.getId(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userMapper.findByEmail(request.email())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return AuthResponse.of(token, user.getId(), user.getEmail(), user.getRole().name());
    }
}
