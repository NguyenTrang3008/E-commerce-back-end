package fs.fresher.SystemE_commerce.validator;

import fs.fresher.SystemE_commerce.dto.request.AddCartItemRequest;
import fs.fresher.SystemE_commerce.dto.request.UpdateCartItemRequest;
import fs.fresher.SystemE_commerce.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CartValidator {
    
    /**
     * Validate add cart item request
     */
    public void validateAddCartItemRequest(AddCartItemRequest request) {
        List<String> errors = new ArrayList<>();
        
        if (request.getSkuId() == null) {
            errors.add("SKU ID is required");
        }
        
        if (request.getQuantity() == null) {
            errors.add("Quantity is required");
        } else if (request.getQuantity() < 1) {
            errors.add("Quantity must be at least 1");
        } else if (request.getQuantity() > 99) {
            errors.add("Quantity cannot exceed 99");
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("Add cart item validation failed", errors);
        }
    }
    
    /**
     * Validate update cart item request
     */
    public void validateUpdateCartItemRequest(UpdateCartItemRequest request) {
        List<String> errors = new ArrayList<>();
        
        if (request.getQuantity() == null) {
            errors.add("Quantity is required");
        } else if (request.getQuantity() < 1) {
            errors.add("Quantity must be at least 1");
        } else if (request.getQuantity() > 99) {
            errors.add("Quantity cannot exceed 99");
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("Update cart item validation failed", errors);
        }
    }
    
    /**
     * Validate cart ID
     */
    public void validateCartId(String cartId, boolean required) {
        if (required && isBlank(cartId)) {
            throw new ValidationException("Cart ID is required");
        }
        
        if (cartId != null && cartId.trim().length() > 100) {
            throw new ValidationException("Cart ID is too long");
        }
    }
    
    /**
     * Helper method to check if string is blank
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}