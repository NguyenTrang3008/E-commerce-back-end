package fs.fresher.SystemE_commerce.validator;

import fs.fresher.SystemE_commerce.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CommonValidator {
    
    /**
     * Validate pagination parameters
     */
    public void validatePagination(int page, int size) {
        List<String> errors = new ArrayList<>();
        
        if (page < 0) {
            errors.add("Page number cannot be negative");
        }
        
        if (size < 1) {
            errors.add("Page size must be at least 1");
        } else if (size > 100) {
            errors.add("Page size cannot exceed 100");
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("Pagination validation failed", errors);
        }
    }
    
    /**
     * Validate ID parameter
     */
    public void validateId(Long id, String fieldName) {
        if (id == null) {
            throw new ValidationException(fieldName + " is required");
        }
        
        if (id <= 0) {
            throw new ValidationException(fieldName + " must be positive");
        }
    }
    
    /**
     * Validate string parameter
     */
    public void validateString(String value, String fieldName, boolean required, int minLength, int maxLength) {
        if (required && isBlank(value)) {
            throw new ValidationException(fieldName + " is required");
        }
        
        if (value != null) {
            int length = value.trim().length();
            if (length < minLength) {
                throw new ValidationException(fieldName + " must be at least " + minLength + " characters");
            }
            if (length > maxLength) {
                throw new ValidationException(fieldName + " cannot exceed " + maxLength + " characters");
            }
        }
    }
    
    /**
     * Helper method to check if string is blank
     */
    public boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}