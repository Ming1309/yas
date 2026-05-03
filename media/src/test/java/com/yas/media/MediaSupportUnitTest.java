package com.yas.media;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.media.config.SecurityConfig;
import com.yas.media.utils.FileTypeValidator;
import com.yas.media.utils.StringUtils;
import com.yas.media.utils.ValidFileType;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@DisplayName("Media support tests")
class MediaSupportUnitTest {

    @Test
    @DisplayName("StringUtils.hasText should handle null blank and text")
    void hasText_shouldHandleCommonInputs() {
        assertFalse(StringUtils.hasText(null));
        assertFalse(StringUtils.hasText("   "));
        assertTrue(StringUtils.hasText("media"));
    }

    @Test
    @DisplayName("FileTypeValidator should accept valid image content")
    void fileTypeValidator_shouldAcceptValidImage() throws IOException {
        FileTypeValidator validator = new FileTypeValidator();
        validator.initialize(validFileType("image/png", "image/png is required"));

        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", createPngBytes());

        assertTrue(validator.isValid(file, context));
    }

    @Test
    @DisplayName("FileTypeValidator should reject null and invalid inputs")
    void fileTypeValidator_shouldRejectInvalidInputs() throws IOException {
        FileTypeValidator validator = new FileTypeValidator();
        validator.initialize(validFileType("image/png", "File type not allowed"));

        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate("File type not allowed")).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(context);

        assertFalse(validator.isValid(null, context));
        verify(context).disableDefaultConstraintViolation();

        MockMultipartFile missingType = new MockMultipartFile("file", "image.png", null, new byte[] {1});
        assertFalse(validator.isValid(missingType, context));

        MockMultipartFile wrongType = new MockMultipartFile("file", "document.pdf", "application/pdf", new byte[] {1});
        assertFalse(validator.isValid(wrongType, context));

        MockMultipartFile invalidImage = new MockMultipartFile("file", "image.png", "image/png", new byte[] {1, 2, 3});
        assertFalse(validator.isValid(invalidImage, context));

        org.springframework.web.multipart.MultipartFile throwingFile = mock(org.springframework.web.multipart.MultipartFile.class);
        when(throwingFile.getContentType()).thenReturn("image/png");
        when(throwingFile.getInputStream()).thenThrow(new IOException("boom"));
        assertFalse(validator.isValid(throwingFile, context));
    }

    @Test
    @DisplayName("SecurityConfig should map realm roles to authorities")
    void securityConfig_shouldMapJwtRoles() {
        SecurityConfig securityConfig = new SecurityConfig();
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverterForKeycloak();

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("realm_access", Map.of("roles", List.of("ADMIN", "MEDIA_EDITOR")))
            .build();

        Collection<? extends GrantedAuthority> authorities = converter.convert(jwt).getAuthorities();

        assertNotNull(authorities);
        assertTrue(authorities.stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(authorities.stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_MEDIA_EDITOR")));
    }

    private static ValidFileType validFileType(String allowedType, String message) {
        return new ValidFileType() {
            @Override
            public String message() {
                return message;
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public String[] allowedTypes() {
                return new String[] {allowedType};
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ValidFileType.class;
            }
        };
    }

    private static byte[] createPngBytes() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }
}