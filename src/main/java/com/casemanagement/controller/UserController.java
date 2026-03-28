package com.casemanagement.controller;

import com.casemanagement.dto.request.ChangePasswordRequest;
import com.casemanagement.dto.request.CreateUserRequest;
import com.casemanagement.dto.request.UpdateUserRequest;
import com.casemanagement.dto.response.ApiResponse;
import com.casemanagement.dto.response.UserResponse;
import com.casemanagement.security.UserPrincipal;
import com.casemanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User CRUD and profile operations")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // ─── Admin: full CRUD ────────────────────────────────────────────────────

    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Create a new user")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", user));
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] List all users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }

    @GetMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(userId)));
    }

    @PutMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Update user (name, email, phone, role, password)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated", userService.updateUserByAdmin(userId, request)));
    }

    @DeleteMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Delete user")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }

    // ─── Authenticated user: own profile ─────────────────────────────────────

    @GetMapping("/users/me")
    @Operation(summary = "Get own profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(principal.getId())));
    }

    @PatchMapping("/users/me")
    @Operation(summary = "Update own profile (name and/or phone only)")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse updated = userService.updateOwnProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", updated));
    }

    @PatchMapping("/users/me/change-password")
    @Operation(summary = "Change own password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}
