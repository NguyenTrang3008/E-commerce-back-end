package fs.fresher.SystemE_commerce.validator;

import fs.fresher.SystemE_commerce.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class CheckoutValidator {
    
    /**
     * Validate checkout token
     */
    public void validateCheckoutToken(String token) {
        if (isBlank(token)) {
            throw new ValidationException("Checkout token is required");
        }
        
        if (token.trim().length() < 10) {
            throw new ValidationException("Invalid checkout token format");
        }
    }
    
    /**
     * Helper method to check if string is blank
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}