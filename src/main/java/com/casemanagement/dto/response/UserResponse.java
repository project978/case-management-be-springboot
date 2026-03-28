package com.casemanagement.dto.response;

import com.casemanagement.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private String id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
