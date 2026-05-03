package com.yas.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.exception.UnsupportedMediaTypeException;
import com.yas.media.exception.ControllerAdvisor;
import com.yas.media.viewmodel.ErrorVm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.core.MethodParameter;

@DisplayName("ControllerAdvisor Unit Tests")
class ControllerAdvisorUnitTest {

    private final ControllerAdvisor controllerAdvisor = new ControllerAdvisor();

    @Test
    @DisplayName("Should handle unsupported media type exception")
    void handleUnsupportedMediaType_shouldReturnBadRequest() {
        WebRequest request = webRequest("/medias");

        ResponseEntity<ErrorVm> response = controllerAdvisor.handleUnsupportedMediaTypeException(
            new UnsupportedMediaTypeException("unsupported"),
            request
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("File uploaded media type is not supported", response.getBody().detail());
        assertEquals("Unsupported media type", response.getBody().title());
    }

    @Test
    @DisplayName("Should handle not found exception")
    void handleNotFound_shouldReturnNotFound() {
        WebRequest request = webRequest("/medias/1");

        ResponseEntity<ErrorVm> response = controllerAdvisor.handleNotFoundException(
            new NotFoundException("Media 1 is not found"),
            request
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Media 1 is not found", response.getBody().detail());
    }

    @Test
    @DisplayName("Should handle runtime exception")
    void handleRuntime_shouldReturnInternalServerError() {
        WebRequest request = webRequest("/medias/1/file/test.jpg");

        ResponseEntity<ErrorVm> response = controllerAdvisor.handleIoException(
            new RuntimeException("boom"),
            request
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("boom", response.getBody().detail());
        assertEquals("RuntimeException", response.getBody().title());
    }

    @Test
    @DisplayName("Should handle generic exception")
    void handleOther_shouldReturnInternalServerError() {
        WebRequest request = webRequest("/medias/1/file/test.jpg");

        ResponseEntity<ErrorVm> response = controllerAdvisor.handleOtherException(
            new Exception("other"),
            request
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("other", response.getBody().detail());
    }

    @Test
    @DisplayName("Should handle method argument not valid")
    void handleMethodArgumentNotValid_shouldReturnBadRequest() throws Exception {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "mediaPostVm");
        bindingResult.addError(new FieldError("mediaPostVm", "caption", "must not be blank"));
        Method method = ControllerAdvisorUnitTest.class.getDeclaredMethod("sample", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorVm> response = controllerAdvisor.handleMethodArgumentNotValid(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Request information is not valid", response.getBody().detail());
        assertTrue(response.getBody().fieldErrors().contains("caption must not be blank"));
    }

    @Test
    @DisplayName("Should handle constraint violation")
    void handleConstraintViolation_shouldReturnBadRequest() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getRootBeanClass()).thenReturn((Class) ControllerAdvisorUnitTest.class);
        when(violation.getPropertyPath()).thenReturn(() -> "caption");
        when(violation.getMessage()).thenReturn("must not be blank");

        ResponseEntity<ErrorVm> response = controllerAdvisor.handleConstraintViolation(
            new ConstraintViolationException(Set.of(violation))
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().fieldErrors().get(0).contains("caption"));
    }

    private void sample(String value) {
    }

    private static WebRequest webRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath(path);
        return new ServletWebRequest(request);
    }
}