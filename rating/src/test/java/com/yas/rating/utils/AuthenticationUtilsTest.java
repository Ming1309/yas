package com.yas.rating.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.AccessDeniedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuthenticationUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extractUserId_whenJwtAuthenticationExists_shouldReturnSubject() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("nhu-user");
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        String userId = AuthenticationUtils.extractUserId();

        assertThat(userId).isEqualTo("nhu-user");
    }

    @Test
    void extractUserId_whenAnonymousAuthentication_shouldThrowAccessDeniedException() {
        SecurityContextHolder.getContext().setAuthentication(
            new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))
        );

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, AuthenticationUtils::extractUserId);

        assertThat(exception.getMessage()).isEqualTo(Constants.ErrorCode.ACCESS_DENIED);
    }
}
