package com.nirmala.logsense.exception;

import com.nirmala.logsense.dto.ErrorResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
