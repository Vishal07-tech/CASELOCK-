package com.caselock.service;

import com.caselock.config.JwtProperties;
import com.caselock.config.SecurityProperties;
import com.caselock.dto.request.LoginRequest;
import com.caselock.dto.request.RegisterRequest;
import com.caselock.dto.response.AuthResponse;
import com.caselock.dto.response.UserResponse;
import com.caselock.entity.LoginHistory;
import com.caselock.entity.User;
import com.caselock.entity.enums.AccountStatus;
import com.caselock.entity.enums.AuditAction;
import com.caselock.entity.enums.AuditResult;
import com.caselock.entity.enums.Role;
import com.caselock.exception.ConflictException;
import com.caselock.exception.UnauthorizedException;
import com.caselock.repository.LoginHistoryRepository;
import com.caselock.repository.UserRepository;
import com.caselock.security.JwtUtil;
import com.caselock.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;
    private final SecurityProperties securityProperties;
    private final JwtProperties jwtProperties;

    @Transactional
    public UserResponse register(RegisterRequest request) {
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
                .role(request.role() != null ? request.role() : Role.VIEWER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        auditLogService.record(user.getId(), AuditAction.USER_CREATED, "User", user.getId(),
                "New account registered for " + user.getUsername(), null, AuditResult.SUCCESS);

        return UserResponse.from(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByUsername(request.username()).orElse(null);

        if (user == null) {
            recordFailedAttempt(request.username(), ipAddress, userAgent, "No such user");
            throw new UnauthorizedException("Invalid username or password.", "INVALID_CREDENTIALS");
        }

        if (user.isLocked()) {
            recordFailedAttempt(request.username(), ipAddress, userAgent, "Account locked");
            throw new UnauthorizedException(
                    "This account is temporarily locked due to repeated failed login attempts. Please try again later.",
                    "ACCOUNT_LOCKED");
        }

        if (user.getAccountStatus() == AccountStatus.DISABLED) {
            recordFailedAttempt(request.username(), ipAddress, userAgent, "Account disabled");
            throw new UnauthorizedException("This account has been disabled. Contact an administrator.", "ACCOUNT_DISABLED");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedPassword(user, ipAddress, userAgent);
            throw new UnauthorizedException("Invalid username or password.", "INVALID_CREDENTIALS");
        }

        // Successful login: reset lockout counters, record history, issue tokens.
        user.setFailedLoginAttempts(0);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        loginHistoryRepository.save(LoginHistory.builder()
                .user(user)
                .username(user.getUsername())
                .successful(true)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .attemptedAt(LocalDateTime.now())
                .build());

        auditLogService.record(user.getId(), AuditAction.LOGIN, "User", user.getId(),
                "User logged in successfully.", ipAddress, AuditResult.SUCCESS);

        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtUtil.generateAccessToken(principal);
        String refreshToken = jwtUtil.generateRefreshToken(principal);

        return AuthResponse.of(accessToken, refreshToken, jwtProperties.expirationMs(), UserResponse.from(user));
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        String tokenType;
        String username;
        try {
            tokenType = jwtUtil.extractTokenType(refreshToken);
            username = jwtUtil.extractUsername(refreshToken);
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid or expired refresh token.", "INVALID_REFRESH_TOKEN");
        }

        if (!"refresh".equals(tokenType) || !jwtUtil.isTokenValid(refreshToken, username)) {
            throw new UnauthorizedException("Invalid or expired refresh token.", "INVALID_REFRESH_TOKEN");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token.", "INVALID_REFRESH_TOKEN"));

        UserPrincipal principal = new UserPrincipal(user);
        String newAccessToken = jwtUtil.generateAccessToken(principal);
        String newRefreshToken = jwtUtil.generateRefreshToken(principal);
        return AuthResponse.of(newAccessToken, newRefreshToken, jwtProperties.expirationMs(), UserResponse.from(user));
    }

    @Transactional
    public void logout(Long userId, String ipAddress) {
        auditLogService.record(userId, AuditAction.LOGOUT, "User", userId,
                "User logged out.", ipAddress, AuditResult.SUCCESS);
    }

    private void handleFailedPassword(User user, String ipAddress, String userAgent) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= securityProperties.maxFailedLoginAttempts()) {
            user.setAccountStatus(AccountStatus.LOCKED);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(securityProperties.accountLockoutMinutes()));
        }
        userRepository.save(user);
        recordFailedAttempt(user.getUsername(), ipAddress, userAgent, "Incorrect password");

        auditLogService.record(user.getId(), AuditAction.LOGIN_FAILED, "User", user.getId(),
                "Failed login attempt (" + attempts + "/" + securityProperties.maxFailedLoginAttempts() + ").",
                ipAddress, AuditResult.FAILURE);
    }

    private void recordFailedAttempt(String username, String ipAddress, String userAgent, String reason) {
        loginHistoryRepository.save(LoginHistory.builder()
                .username(username)
                .successful(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .failureReason(reason)
                .attemptedAt(LocalDateTime.now())
                .build());
    }
}
