package fs.fresher.SystemE_commerce.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage(), ex);
        
        String message = "Data integrity violation occurred";
        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
        
        // Parse specific constraint violations
        String rootCauseMessage = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        
        if (rootCauseMessage != null) {
            if (rootCauseMessage.contains("order_number") && rootCauseMessage.contains("unique")) {
                message = "Order number already exists. Please try again.";
                errorCode = ErrorCode.DUPLICATE_RESOURCE;
            } else if (rootCauseMessage.contains("tracking_token") && rootCauseMessage.contains("unique")) {
                message = "Tracking token already exists. Please try again.";
                errorCode = ErrorCode.DUPLICATE_RESOURCE;
            } else if (rootCauseMessage.contains("not-null") || rootCauseMessage.contains("cannot be null")) {
                message = "Required field is missing or null";
                errorCode = ErrorCode.VALIDATION_FAILED;
            } else if (rootCauseMessage.contains("foreign key constraint")) {
                message = "Referenced data not found or invalid";
                errorCode = ErrorCode.RESOURCE_NOT_FOUND;
            } else if (rootCauseMessage.contains("duplicate key") || rootCauseMessage.contains("unique constraint")) {
                message = "Duplicate data detected. Please check your input.";
                errorCode = ErrorCode.DUPLICATE_RESOURCE;
            }
        }
        
        ErrorResponse errorResponse = ErrorResponse.of(
            errorCode,
            message,
            getPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("Business exception: {} - {}", ex.getCode(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ex.getErrorCode(), 
            ex.getMessage(), 
            getPath(request)
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        for (int i = 0; i < ex.getErrors().size(); i++) {
            details.put("error" + (i + 1), ex.getErrors().get(i));
        }
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ex.getErrorCode(),
            ex.getMessage(),
            getPath(request),
            details
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Method argument validation failed: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.VALIDATION_FAILED,
            "Request validation failed",
            getPath(request),
            details
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.RESOURCE_NOT_FOUND,
            ex.getMessage(),
            getPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
        log.error("Unexpected runtime exception", ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            getPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected exception", ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            getPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}