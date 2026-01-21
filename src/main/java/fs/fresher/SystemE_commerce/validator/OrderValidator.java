package fs.fresher.SystemE_commerce.validator;

import fs.fresher.SystemE_commerce.dto.request.PlaceOrderRequest;
import fs.fresher.SystemE_commerce.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class OrderValidator {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[0-9+\\-\\s()]{10,15}$"
    );
    
    /**
     * Validate place order request
     */
    public void validatePlaceOrderRequest(PlaceOrderRequest request) {
        List<String> errors = new ArrayList<>();
        
        // Customer name validation
        if (isBlank(request.getCustomerName())) {
            errors.add("Customer name is required");
        } else if (request.getCustomerName().trim().length() < 2) {
            errors.add("Customer name must be at least 2 characters");
        }
        
        // Phone validation
        if (isBlank(request.getCustomerPhone())) {
            errors.add("Phone number is required");
        } else if (!PHONE_PATTERN.matcher(request.getCustomerPhone().trim()).matches()) {
            errors.add("Invalid phone number format");
        }
        
        // Email validation
        if (isBlank(request.getCustomerEmail())) {
            errors.add("Email is required");
        } else if (!EMAIL_PATTERN.matcher(request.getCustomerEmail().trim()).matches()) {
            errors.add("Invalid email format");
        }
        
        // Shipping address validation
        if (isBlank(request.getShippingAddress())) {
            errors.add("Shipping address is required");
        } else if (request.getShippingAddress().trim().length() < 10) {
            errors.add("Shipping address must be at least 10 characters");
        }
        
        // Payment method validation
        if (request.getPaymentMethod() == null) {
            errors.add("Payment method is required");
        }
        
        // Cart/checkout validation
        if (isBlank(request.getCartId()) && isBlank(request.getCheckoutToken())) {
            errors.add("Either cart ID or checkout token is required");
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("Order validation failed", errors);
        }
    }
    
    /**
     * Validate order number format
     */
    public void validateOrderNumber(String orderNumber) {
        if (isBlank(orderNumber)) {
            throw new ValidationException("Order number is required");
        }
        
        if (!orderNumber.matches("^ORD\\d{14}\\d{4}$")) {
            throw new ValidationException("Invalid order number format");
        }
    }
    
    /**
     * Validate tracking token
     */
    public void validateTrackingToken(String token) {
        if (isBlank(token)) {
            throw new ValidationException("Tracking token is required");
        }
        
        if (token.trim().length() < 10) {
            throw new ValidationException("Invalid tracking token format");
        }
    }
    
    /**
     * Helper method to check if string is blank
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}