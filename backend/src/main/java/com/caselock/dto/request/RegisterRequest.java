package com.caselock.dto.request;

import com.caselock.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50, message = "Username must be 3-50 characters") String username,
        @NotBlank @Email(message = "A valid email address is required") String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        @NotBlank @Size(max = 120) String fullName,
        String badgeNumber,
        String department,
        String phoneNumber,
        @NotNull(message = "Role is required") Role role
) {
}
