package com.caselock.util;

import com.caselock.exception.UnauthorizedException;
import com.caselock.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Small helper for pulling the currently authenticated user out of the
 * Spring Security context. Used throughout the service layer so every
 * mutating operation can record who performed it.
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("No authenticated user found in the current request context.");
        }
        return principal;
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }

    public static String currentUsername() {
        return currentUser().getUsername();
    }
}
