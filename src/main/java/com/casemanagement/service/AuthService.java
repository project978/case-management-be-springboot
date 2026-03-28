package com.casemanagement.service;

import com.casemanagement.dto.request.LoginRequest;
import com.casemanagement.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
}
