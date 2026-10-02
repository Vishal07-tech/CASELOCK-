package com.caselock.service;

import com.caselock.dto.request.PasswordResetRequest;
import com.caselock.dto.request.RegisterRequest;
import com.caselock.dto.request.UserUpdateRequest;
import com.caselock.dto.response.LoginHistoryResponse;
import com.caselock.dto.response.PageResponse;
import com.caselock.dto.response.UserResponse;
import com.caselock.entity.User;
import com.caselock.entity.enums.AccountStatus;
import com.caselock.entity.enums.AuditAction;
import com.caselock.entity.enums.AuditResult;
import com.caselock.entity.enums.Role;
import com.caselock.exception.ConflictException;
import com.caselock.exception.ResourceNotFoundException;
import com.caselock.repository.LoginHistoryRepository;
import com.caselock.repository.UserRepository;
import com.caselock.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional
    public UserResponse createUser(RegisterRequest request, String ipAddress) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("This username is already taken.", "USERNAME_TAKEN");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("An account already exists with this email address.", "EMAIL_TAKEN");
        }
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .badgeNumber(request.badgeNumber())
                .department(request.department())
                .phoneNumber(request.phoneNumber())
                .role(request.role())
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        auditLogService.record(SecurityUtil.currentUserId(), AuditAction.USER_CREATED, "User", user.getId(),
                "Administrator created user account " + user.getUsername() + " with role " + user.getRole(),
                ipAddress, AuditResult.SUCCESS);

        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return UserResponse.from(findUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String keyword, Role role, Pageable pageable) {
        Page<User> page = userRepository.search(keyword, role, pageable);
        return PageResponse.from(page.map(UserResponse::from));
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request, String ipAddress) {
        User user = findUserOrThrow(id);
        Role previousRole = user.getRole();

        if (request.email() != null) user.setEmail(request.email());
        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.badgeNumber() != null) user.setBadgeNumber(request.badgeNumber());
        if (request.department() != null) user.setDepartment(request.department());
        if (request.phoneNumber() != null) user.setPhoneNumber(request.phoneNumber());
        if (request.accountStatus() != null) user.setAccountStatus(request.accountStatus());
        if (request.role() != null) user.setRole(request.role());

        user = userRepository.save(user);

        auditLogService.record(SecurityUtil.currentUserId(), AuditAction.USER_UPDATED, "User", user.getId(),
                "User profile updated.", ipAddress, AuditResult.SUCCESS);

        if (request.role() != null && request.role() != previousRole) {
            auditLogService.record(SecurityUtil.currentUserId(), AuditAction.ROLE_CHANGED, "User", user.getId(),
                    "Role changed from " + previousRole + " to " + request.role() + ".", ipAddress, AuditResult.SUCCESS);
        }

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse disableUser(Long id, String ipAddress) {
        User user = findUserOrThrow(id);
        user.setAccountStatus(AccountStatus.DISABLED);
        user = userRepository.save(user);

        auditLogService.record(SecurityUtil.currentUserId(), AuditAction.USER_DISABLED, "User", user.getId(),
                "User account disabled.", ipAddress, AuditResult.SUCCESS);

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse enableUser(Long id, String ipAddress) {
        User user = findUserOrThrow(id);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user = userRepository.save(user);

        auditLogService.record(SecurityUtil.currentUserId(), AuditAction.USER_UPDATED, "User", user.getId(),
                "User account re-enabled.", ipAddress, AuditResult.SUCCESS);

        return UserResponse.from(user);
    }

    @Transactional
    public void resetPassword(Long id, PasswordResetRequest request, String ipAddress) {
        User user = findUserOrThrow(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        if (user.getAccountStatus() == AccountStatus.LOCKED) {
            user.setAccountStatus(AccountStatus.ACTIVE);
        }
        userRepository.save(user);

        auditLogService.record(SecurityUtil.currentUserId(), AuditAction.PASSWORD_RESET, "User", user.getId(),
                "Password reset by administrator.", ipAddress, AuditResult.SUCCESS);
    }

    @Transactional(readOnly = true)
    public PageResponse<LoginHistoryResponse> loginHistory(Long userId, Pageable pageable) {
        Page<com.caselock.entity.LoginHistory> page = loginHistoryRepository.findByUserIdOrderByAttemptedAtDesc(userId, pageable);
        return PageResponse.from(page.map(LoginHistoryResponse::from));
    }

    User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found.", "USER_NOT_FOUND"));
    }
}
