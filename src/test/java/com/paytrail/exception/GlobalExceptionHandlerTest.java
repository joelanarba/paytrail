package com.paytrail.exception;

import com.paytrail.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unauthorizedMapsTo401() {
        ResponseEntity<ApiResponse<Void>> r = handler.handleUnauthorized(new UnauthorizedException("no key"));
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
        assertFalse(r.getBody().isSuccess());
        assertEquals("no key", r.getBody().getMessage());
    }

    @Test
    void notFoundMapsTo404() {
        assertEquals(HttpStatus.NOT_FOUND,
            handler.handleNotFound(new ResourceNotFoundException("x")).getStatusCode());
    }

    @Test
    void invalidSignatureMapsTo400() {
        assertEquals(HttpStatus.BAD_REQUEST,
            handler.handleInvalidSignature(new InvalidSignatureException("bad")).getStatusCode());
    }

    @Test
    void forbiddenMapsTo403() {
        assertEquals(HttpStatus.FORBIDDEN,
            handler.handleForbidden(new ForbiddenException("x")).getStatusCode());
    }

    @Test
    void duplicateEventMapsTo409() {
        assertEquals(HttpStatus.CONFLICT,
            handler.handleDuplicate(new DuplicateEventException("x")).getStatusCode());
    }

    @Test
    void noResourceMapsTo404() {
        var ex = new org.springframework.web.servlet.resource.NoResourceFoundException(
            org.springframework.http.HttpMethod.GET, "/api/v1/nope");
        assertEquals(HttpStatus.NOT_FOUND, handler.handleNoResource(ex).getStatusCode());
    }

    @Test
    void validationErrorsMapsTo400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(List.of(new FieldError("obj", "email", "must not be blank")));
        var r = handler.handleValidation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertFalse(r.getBody().getErrors().isEmpty());
    }
}
