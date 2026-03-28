package com.casemanagement.service.impl;

import com.casemanagement.dto.request.LoginRequest;
import com.casemanagement.dto.response.AuthResponse;
import com.casemanagement.entity.User;
import com.casemanagement.repository.UserRepository;
import com.casemanagement.security.JwtUtil;
import com.casemanagement.security.UserPrincipal;
import com.casemanagement.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();
        String token = jwtUtil.generateToken(principal);

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .build();
    }
}
