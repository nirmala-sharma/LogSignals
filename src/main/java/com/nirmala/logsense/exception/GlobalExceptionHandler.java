package com.nirmala.logsense.exception;

import com.nirmala.logsense.dto.ErrorResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // handles empty file
    @ExceptionHandler(EmptyLogFileException.class)
    public ResponseEntity<ErrorResponseDTO> handleEmptyFile(EmptyLogFileException e) {
        log.error("Empty file error: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)  // 400
                .body(new ErrorResponseDTO(
                        "error",
                        "EMPTY_LOG_FILE",
                        e.getMessage()
                ));
    }

    // handles log parse errors
    @ExceptionHandler(LogParseException.class)
    public ResponseEntity<ErrorResponseDTO> handleLogParse(LogParseException e) {
        log.error("Log parse error: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)  // 422
                .body(new ErrorResponseDTO(
                        "error",
                        "LOG_PARSE_ERROR",
                        e.getMessage()
                ));
    }

    // handles analysis errors
    @ExceptionHandler(LogAnalysisException.class)
    public ResponseEntity<ErrorResponseDTO> handleAnalysisError(LogAnalysisException e) {
        log.error("Analysis error: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)  // 500
                .body(new ErrorResponseDTO(
                        "error",
                        "ANALYSIS_FAILED",
                        e.getMessage()
                ));
    }

    // handles file too large
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleFileTooLarge(MaxUploadSizeExceededException e) {
        log.error("File too large: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)  // 413
                .body(new ErrorResponseDTO(
                        "error",
                        "FILE_TOO_LARGE",
                        "Uploaded file exceeds maximum allowed size"
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationError(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed");

        log.error("Validation error: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(
                        "error",
                        "VALIDATION_ERROR",
                        message
                ));
    }

    // invalid login, missing/invalid API key or access token
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthentication(AuthenticationException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)  // 401
                .body(new ErrorResponseDTO(
                        "error",
                        "UNAUTHORIZED",
                        e.getMessage()
                ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)  // 404
                .body(new ErrorResponseDTO(
                        "error",
                        "NOT_FOUND",
                        e.getMessage()
                ));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseDTO> handleConflict(ConflictException e) {
        log.warn("Conflict: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)  // 409
                .body(new ErrorResponseDTO(
                        "error",
                        "CONFLICT",
                        e.getMessage()
                ));
    }

    // safety net: a database unique constraint was violated (e.g. two identical requests at the same time)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)  // 409
                .body(new ErrorResponseDTO(
                        "error",
                        "CONFLICT",
                        "This record already exists or conflicts with existing data"
                ));
    }

    // request body is not valid JSON (e.g. a value without quotes)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.warn("Unreadable request body: {}", e.getMessage());
        return badRequest("INVALID_JSON",
                "Request body is not valid JSON. Check quotes, commas and brackets.");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingHeader(MissingRequestHeaderException e) {
        log.warn("Missing header: {}", e.getHeaderName());
        return badRequest("MISSING_HEADER", "Missing required header: " + e.getHeaderName());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingPart(MissingServletRequestPartException e) {
        log.warn("Missing multipart part: {}", e.getRequestPartName());
        return badRequest("MISSING_FILE", "Missing required part: " + e.getRequestPartName());
    }

    // unknown URL (e.g. browser asking for favicon/apple-touch-icon) -> 404, not 500
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDTO("error", "NOT_FOUND", "Resource not found"));
    }

    private ResponseEntity<ErrorResponseDTO> badRequest(String code, String message) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)  // 400
                .body(new ErrorResponseDTO("error", code, message));
    }

    // catches everything else — fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneral(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)  // 500
                .body(new ErrorResponseDTO(
                        "error",
                        "INTERNAL_ERROR",
                        "An unexpected error occurred"
                ));
    }
}
