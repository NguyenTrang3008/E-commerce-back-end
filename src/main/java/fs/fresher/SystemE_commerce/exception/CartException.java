package fs.fresher.SystemE_commerce.exception;

/**
 * Exception thrown for cart-related errors
 */
public class CartException extends BusinessException {
    
    public CartException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public CartException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
    
    public CartException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
    }
    
    // Convenience methods for common cart errors
    public static CartException empty(String cartId) {
        return new CartException(ErrorCode.CART_EMPTY, 
            String.format("Cart %s is empty", cartId));
    }
    
    public static CartException itemNotFound(String cartId, Long variantId) {
        return new CartException(ErrorCode.CART_ITEM_NOT_FOUND, 
            String.format("Item with variant id %d not found in cart %s", variantId, cartId));
    }
    
    public static CartException invalidQuantity(int quantity) {
        return new CartException(ErrorCode.INVALID_QUANTITY, 
            String.format("Invalid quantity: %d. Quantity must be greater than 0", quantity));
    }
}