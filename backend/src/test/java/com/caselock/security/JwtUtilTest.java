package com.caselock.security;

import com.caselock.config.JwtProperties;
import com.caselock.entity.User;
import com.caselock.entity.enums.AccountStatus;
import com.caselock.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "test-secret-key-value-that-is-long-enough-for-hs256-signing-1234567890",
                60_000L,   // 1 minute access token
                120_000L   // 2 minute refresh token
        );
        jwtUtil = new JwtUtil(properties);

        // id lives on BaseEntity (a @MappedSuperclass); User's plain @Builder
        // does not expose inherited superclass fields, so it has to be set via
        // the inherited setter after building rather than in the chain above.
        User user = User.builder()
                .username("investigator").role(Role.INVESTIGATOR)
                .accountStatus(AccountStatus.ACTIVE).passwordHash("hash")
                .build();
        user.setId(1L);
        principal = new UserPrincipal(user);
    }

    @Test
    void generatedAccessTokenIsValidForItsOwner() {
        String token = jwtUtil.generateAccessToken(principal);

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("investigator");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(1L);
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo("access");
        assertThat(jwtUtil.isTokenValid(token, "investigator")).isTrue();
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
    }

    @Test
    void tokenIsNotValidForADifferentUsername() {
        String token = jwtUtil.generateAccessToken(principal);
        assertThat(jwtUtil.isTokenValid(token, "someone-else")).isFalse();
    }

    @Test
    void refreshTokenIsTaggedDifferentlyFromAccessToken() {
        String refreshToken = jwtUtil.generateRefreshToken(principal);
        assertThat(jwtUtil.extractTokenType(refreshToken)).isEqualTo("refresh");
    }
}
