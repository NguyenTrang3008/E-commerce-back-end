package fs.fresher.SystemE_commerce.validator;

import fs.fresher.SystemE_commerce.dto.request.AddCartItemRequest;
import fs.fresher.SystemE_commerce.dto.request.PlaceOrderRequest;
import fs.fresher.SystemE_commerce.dto.request.UpdateCartItemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Main validation service that delegates to specific validators
 * This acts as a facade for all validation operations
 */
@Service
@RequiredArgsConstructor
public class ValidationService {
    
    private final OrderValidator orderValidator;
    private final CartValidator cartValidator;
    private final CheckoutValidator checkoutValidator;
    private final CommonValidator commonValidator;
    
    // Order validations
    public void validatePlaceOrderRequest(PlaceOrderRequest request) {
        orderValidator.validatePlaceOrderRequest(request);
    }
    
    public void validateOrderNumber(String orderNumber) {
        orderValidator.validateOrderNumber(orderNumber);
    }
    
    public void validateTrackingToken(String token) {
        orderValidator.validateTrackingToken(token);
    }
    
    // Cart validations
    public void validateAddCartItemRequest(AddCartItemRequest request) {
        cartValidator.validateAddCartItemRequest(request);
    }
    
    public void validateUpdateCartItemRequest(UpdateCartItemRequest request) {
        cartValidator.validateUpdateCartItemRequest(request);
    }
    
    public void validateCartId(String cartId, boolean required) {
        cartValidator.validateCartId(cartId, required);
    }
    
    // Checkout validations
    public void validateCheckoutToken(String token) {
        checkoutValidator.validateCheckoutToken(token);
    }
    
    // Common validations
    public void validatePagination(int page, int size) {
        commonValidator.validatePagination(page, size);
    }
    
    public void validateId(Long id, String fieldName) {
        commonValidator.validateId(id, fieldName);
    }
    
    public void validateString(String value, String fieldName, boolean required, int minLength, int maxLength) {
        commonValidator.validateString(value, fieldName, required, minLength, maxLength);
    }
}