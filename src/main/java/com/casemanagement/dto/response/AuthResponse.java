package com.casemanagement.dto.response;

import com.casemanagement.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
}
