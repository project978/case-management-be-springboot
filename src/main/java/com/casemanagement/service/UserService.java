package com.casemanagement.service;

import com.casemanagement.dto.request.ChangePasswordRequest;
import com.casemanagement.dto.request.CreateUserRequest;
import com.casemanagement.dto.request.UpdateUserRequest;
import com.casemanagement.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    // Admin operations
    UserResponse createUser(CreateUserRequest request);
    UserResponse updateUserByAdmin(String userId, CreateUserRequest request);
    void deleteUser(String userId);
    List<UserResponse> getAllUsers();
    UserResponse getUserById(String userId);

    // User operations (own profile)
    UserResponse updateOwnProfile(String userId, UpdateUserRequest request);
    void changePassword(String userId, ChangePasswordRequest request);
}
