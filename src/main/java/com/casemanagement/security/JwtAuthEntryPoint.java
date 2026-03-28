package com.casemanagement.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("[401] Unauthorized request to '{}': {}", request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("success", false);
        body.put("message", resolveMessage(request, authException));

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private String resolveMessage(HttpServletRequest request, AuthenticationException ex) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            return "Access denied: no Authorization header found. Please login and provide a Bearer token.";
        }
        if (!authHeader.startsWith("Bearer ")) {
            return "Access denied: Authorization header must start with 'Bearer '.";
        }
        // Token was present but invalid/expired
        return "Access denied: your session has expired or the token is invalid. Please login again.";
    }
}
