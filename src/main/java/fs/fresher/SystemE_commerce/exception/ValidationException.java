package fs.fresher.SystemE_commerce.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ValidationException extends BusinessException {
    private final List<String> errors;
    
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
        this.errors = List.of(message);
    }
    
    public ValidationException(String message, List<String> errors) {
        super(ErrorCode.VALIDATION_FAILED, message);
        this.errors = errors;
    }
    
    public ValidationException(List<String> errors) {
        super(ErrorCode.VALIDATION_FAILED, "Validation failed");
        this.errors = errors;
    }
}