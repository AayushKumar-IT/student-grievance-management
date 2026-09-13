package com.grievance.management.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEntry(DataIntegrityViolationException ex) {
        String message = resolveDuplicateMessage(ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + ex.getMessage());
    }

    /**
     * Parses the constraint name from the SQL exception message and returns
     * a human-readable error instead of the raw SQL.
     */
    private String resolveDuplicateMessage(String raw) {
        if (raw == null) return "A duplicate entry already exists.";
        String lower = raw.toLowerCase();

        if (lower.contains("uq_users_email") || lower.contains("users.email"))
            return "This email address is already registered. Please use a different email.";

        if (lower.contains("enrollment_number") || lower.contains("uq_students_enroll"))
            return "This College ID / enrollment number is already registered. Please check your ID.";

        if (lower.contains("employee_id") || lower.contains("uq_faculty_emp_id"))
            return "This Employee ID is already registered. Please check your employee ID.";

        if (lower.contains("uq_colleges_code") || lower.contains("colleges.code"))
            return "A college with this code already exists.";

        if (lower.contains("uq_colleges_name") || lower.contains("colleges.name"))
            return "A college with this name already exists.";

        if (lower.contains("uq_reg_token") || lower.contains("registration_tokens.token"))
            return "This registration token already exists.";

        if (lower.contains("duplicate entry"))
            return "This record already exists. Please check the fields and try again.";

        return "A duplicate entry already exists. Please check your details.";
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        ));
    }
}
