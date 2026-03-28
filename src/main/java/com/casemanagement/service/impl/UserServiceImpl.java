package com.casemanagement.service.impl;

import com.casemanagement.dto.request.ChangePasswordRequest;
import com.casemanagement.dto.request.CreateUserRequest;
import com.casemanagement.dto.request.UpdateUserRequest;
import com.casemanagement.dto.response.UserResponse;
import com.casemanagement.entity.User;
import com.casemanagement.exception.BadRequestException;
import com.casemanagement.exception.ResourceNotFoundException;
import com.casemanagement.repository.UserRepository;
import com.casemanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateUserByAdmin(String userId, CreateUserRequest request) {
        User user = findUserById(userId);

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use: " + request.getEmail());
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        User user = findUserById(userId);
        userRepository.delete(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse getUserById(String userId) {
        return toResponse(findUserById(userId));
    }

    @Override
    @Transactional
    public UserResponse updateOwnProfile(String userId, UpdateUserRequest request) {
        User user = findUserById(userId);

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone());
        }

        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User findUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
