package fs.fresher.SystemE_commerce.exception;

/**
 * Exception thrown for checkout-related errors
 */
public class CheckoutException extends BusinessException {
    
    public CheckoutException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public CheckoutException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
    
    public CheckoutException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
    }
    
    // Convenience methods for common checkout errors
    public static CheckoutException sessionExpired(String sessionId) {
        return new CheckoutException(ErrorCode.CHECKOUT_SESSION_EXPIRED, 
            String.format("Checkout session %s has expired", sessionId));
    }
    
    public static CheckoutException alreadyCompleted(String sessionId) {
        return new CheckoutException(ErrorCode.CHECKOUT_ALREADY_COMPLETED, 
            String.format("Checkout session %s has already been completed", sessionId));
    }
    
    public static CheckoutException sessionNotFound(String sessionId) {
        return new CheckoutException(ErrorCode.CHECKOUT_SESSION_NOT_FOUND, 
            String.format("Checkout session %s not found", sessionId));
    }
}