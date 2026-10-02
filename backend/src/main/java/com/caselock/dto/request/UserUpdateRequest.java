package com.caselock.dto.request;

import com.caselock.entity.enums.AccountStatus;
import com.caselock.entity.enums.Role;
import jakarta.validation.constraints.Email;

public record UserUpdateRequest(
        @Email String email,
        String fullName,
        String badgeNumber,
        String department,
        String phoneNumber,
        Role role,
        AccountStatus accountStatus
) {
}
