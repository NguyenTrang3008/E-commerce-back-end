package fs.fresher.SystemE_commerce.exception;

/**
 * Exception thrown when a requested resource is not found
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public ResourceNotFoundException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
    
    // Convenience constructors for common cases
    public static ResourceNotFoundException product(Long productId) {
        return new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, 
            String.format("Product with id '%s' not found", productId));
    }
    
    public static ResourceNotFoundException productVariant(Long variantId) {
        return new ResourceNotFoundException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, 
            String.format("Product variant with id '%s' not found", variantId));
    }
    
    public static ResourceNotFoundException order(String orderNumber) {
        return new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, 
            String.format("Order with number '%s' not found", orderNumber));
    }
    
    public static ResourceNotFoundException cart(String cartId) {
        return new ResourceNotFoundException(ErrorCode.CART_NOT_FOUND, 
            String.format("Cart with id '%s' not found", cartId));
    }
    
    public static ResourceNotFoundException reservation(String reservationToken) {
        return new ResourceNotFoundException(ErrorCode.RESERVATION_NOT_FOUND, 
            String.format("Reservation with token '%s' not found", reservationToken));
    }
}