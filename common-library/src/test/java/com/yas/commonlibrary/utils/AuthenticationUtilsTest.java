package com.yas.commonlibrary.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.AccessDeniedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.Collections;

class AuthenticationUtilsTest {

    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extractUserId_WhenAuthenticated_ShouldReturnSubject() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("test-user-id");
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        String userId = AuthenticationUtils.extractUserId();
        assertEquals("test-user-id", userId);
    }

    @Test
    void extractUserId_WhenAnonymous_ShouldThrowAccessDeniedException() {
        AnonymousAuthenticationToken anonymousAuth = mock(AnonymousAuthenticationToken.class);
        when(securityContext.getAuthentication()).thenReturn(anonymousAuth);

        assertThrows(AccessDeniedException.class, AuthenticationUtils::extractUserId);
    }

    @Test
    void extractJwt_WhenAuthenticated_ShouldReturnTokenValue() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("test-jwt-token-value");
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        String token = AuthenticationUtils.extractJwt();
        assertEquals("test-jwt-token-value", token);
    }
}
