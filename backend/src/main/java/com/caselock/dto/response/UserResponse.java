package com.caselock.dto.response;

import com.caselock.entity.User;
import com.caselock.entity.enums.AccountStatus;
import com.caselock.entity.enums.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String badgeNumber,
        String department,
        String phoneNumber,
        Role role,
        AccountStatus accountStatus,
        LocalDateTime lastLoginAt,
        String lastLoginIp,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getBadgeNumber(),
                user.getDepartment(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getAccountStatus(),
                user.getLastLoginAt(),
                user.getLastLoginIp(),
                user.getCreatedAt()
        );
    }
}
