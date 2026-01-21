package fs.fresher.SystemE_commerce.exception;

/**
 * Exception thrown for inventory-related errors
 */
public class InventoryException extends BusinessException {
    
    public InventoryException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public InventoryException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
    
    public InventoryException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
    }
    
    // Convenience methods for common inventory errors
    public static InventoryException reservationFailed(Long variantId, int quantity, String reason) {
        return new InventoryException(ErrorCode.STOCK_RESERVATION_FAILED, 
            String.format("Failed to reserve %d units of variant %d: %s", quantity, variantId, reason));
    }
    
    public static InventoryException reservationExpired(String reservationToken) {
        return new InventoryException(ErrorCode.STOCK_RESERVATION_EXPIRED, 
            String.format("Stock reservation %s has expired", reservationToken));
    }
    
    public static InventoryException alreadyConfirmed(String reservationToken) {
        return new InventoryException(ErrorCode.STOCK_ALREADY_CONFIRMED, 
            String.format("Stock reservation %s is already confirmed", reservationToken));
    }
    
    public static InventoryException alreadyReleased(String reservationToken) {
        return new InventoryException(ErrorCode.STOCK_ALREADY_RELEASED, 
            String.format("Stock reservation %s is already released", reservationToken));
    }
}