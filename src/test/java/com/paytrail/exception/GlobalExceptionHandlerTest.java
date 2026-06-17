package com.paytrail.exception;

import com.paytrail.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

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
}
