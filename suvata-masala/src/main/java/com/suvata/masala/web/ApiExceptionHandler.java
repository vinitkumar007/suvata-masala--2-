package com.suvata.masala.web;

import com.suvata.masala.dto.ApiModels.ApiError;
import com.suvata.masala.service.AdminAccessException;
import com.suvata.masala.service.PaymentUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + Objects.requireNonNullElse(f.getDefaultMessage(), "galat value"))
                .toList();
        return ResponseEntity.badRequest().body(new ApiError("Form mein kuch galat hai", details));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ApiError(ex.getMessage(), List.of()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> notFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage(), List.of()));
    }

    @ExceptionHandler(PaymentUnavailableException.class)
    public ResponseEntity<ApiError> paymentDown(PaymentUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(ex.getMessage(), List.of()));
    }

    @ExceptionHandler(AdminAccessException.class)
    public ResponseEntity<ApiError> admin(AdminAccessException ex) {
        HttpStatus status = ex.isDisabled() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(new ApiError(ex.getMessage(), List.of()));
    }
}
