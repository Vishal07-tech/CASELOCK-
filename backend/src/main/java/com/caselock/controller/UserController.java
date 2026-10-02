package com.caselock.controller;

import com.caselock.dto.request.PasswordResetRequest;
import com.caselock.dto.request.RegisterRequest;
import com.caselock.dto.request.UserUpdateRequest;
import com.caselock.dto.response.ApiResponse;
import com.caselock.dto.response.LoginHistoryResponse;
import com.caselock.dto.response.PageResponse;
import com.caselock.dto.response.UserResponse;
import com.caselock.entity.enums.Role;
import com.caselock.service.UserService;
import com.caselock.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody RegisterRequest request,
                                                              HttpServletRequest httpRequest) {
        UserResponse response = userService.createUser(request, RequestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("User created successfully.", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved.", userService.search(keyword, role, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved.", userService.getUser(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id,
                                                              @RequestBody UserUpdateRequest request,
                                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("User updated successfully.",
                userService.updateUser(id, request, RequestUtil.extractClientIp(httpRequest))));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> disable(@PathVariable Long id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("User account disabled.",
                userService.disableUser(id, RequestUtil.extractClientIp(httpRequest))));
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> enable(@PathVariable Long id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("User account enabled.",
                userService.enableUser(id, RequestUtil.extractClientIp(httpRequest))));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id,
                                                             @Valid @RequestBody PasswordResetRequest request,
                                                             HttpServletRequest httpRequest) {
        userService.resetPassword(id, request, RequestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully."));
    }

    @GetMapping("/directory")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> directory(
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 100, sort = "fullName") Pageable pageable) {
        // Lightweight, broadly-accessible listing used to populate dropdowns
        // (e.g. "assign investigator", "transfer to") without exposing the
        // full admin user-management screen to every role.
        return ResponseEntity.ok(ApiResponse.success("Directory retrieved.", userService.search(null, role, pageable)));
    }

    @GetMapping("/{id}/login-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<LoginHistoryResponse>>> loginHistory(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "attemptedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Login history retrieved.", userService.loginHistory(id, pageable)));
    }
}
