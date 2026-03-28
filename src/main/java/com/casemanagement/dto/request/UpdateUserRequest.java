package com.casemanagement.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateUserRequest {

    private String name;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String phone;
}
