package com.socialdashboard.exception;

import com.socialdashboard.dto.Dtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Dtos.ApiError> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new Dtos.ApiError(ex.getMessage(), ex.getField(), ex.getStatus().value()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Dtos.ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        String field = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField())
                .findFirst()
                .orElse(null);
        if (message.isBlank()) {
            message = "Request validation failed";
        }
        return ResponseEntity.badRequest()
                .body(new Dtos.ApiError(message, field, HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Dtos.ApiError> handleNotFound(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Dtos.ApiError.of(ex.getMessage(), HttpStatus.UNAUTHORIZED.value()));
    }

    /**
     * Unmapped URLs arrive here as NoResourceFoundException. Without this the
     * catch-all below would report a typo in the path as a 500.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<Dtos.ApiError> handleUnknownRoute(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Dtos.ApiError.of("No endpoint " + ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    /** A known path hit with the wrong verb, e.g. PATCH on a PUT-only route. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Dtos.ApiError> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Dtos.ApiError.of(ex.getMessage(), HttpStatus.METHOD_NOT_ALLOWED.value()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Dtos.ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(Dtos.ApiError.of(ex.getMessage(), HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<Dtos.ApiError> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Dtos.ApiError.of(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Dtos.ApiError> handleGeneric(Exception ex) {
        // Log the cause: the client only ever sees a generic message, so without
        // this the stack trace is lost and the failure cannot be diagnosed.
        log.error("Unhandled exception while serving a request", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Dtos.ApiError.of("Unexpected server error", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}
