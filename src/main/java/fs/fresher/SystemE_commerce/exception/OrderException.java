package fs.fresher.SystemE_commerce.exception;

/**
 * Exception thrown for order-related errors
 */
public class OrderException extends BusinessException {
    
    public OrderException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public OrderException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
    
    public OrderException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
    }
    
    // Convenience methods for common order errors
    public static OrderException alreadyPlaced(String orderNumber) {
        return new OrderException(ErrorCode.ORDER_ALREADY_PLACED, 
            String.format("Order %s has already been placed", orderNumber));
    }
    
    public static OrderException cannotBeCancelled(String orderNumber, String reason) {
        return new OrderException(ErrorCode.ORDER_CANNOT_BE_CANCELLED, 
            String.format("Order %s cannot be cancelled: %s", orderNumber, reason));
    }
    
    public static OrderException processingFailed(String orderNumber, String reason) {
        return new OrderException(ErrorCode.ORDER_PROCESSING_FAILED, 
            String.format("Order %s processing failed: %s", orderNumber, reason));
    }
    
    public static OrderException invalidStatus(String orderNumber, String currentStatus, String requestedStatus) {
        return new OrderException(ErrorCode.INVALID_ORDER_STATUS, 
            String.format("Cannot change order %s status from %s to %s", orderNumber, currentStatus, requestedStatus));
    }
}