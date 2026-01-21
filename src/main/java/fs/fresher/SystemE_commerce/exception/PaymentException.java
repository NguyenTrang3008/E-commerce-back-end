package fs.fresher.SystemE_commerce.exception;

/**
 * Exception thrown for payment-related errors
 */
public class PaymentException extends BusinessException {
    
    public PaymentException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public PaymentException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
    
    public PaymentException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
    }
    
    // Convenience methods for common payment errors
    public static PaymentException failed(String reason) {
        return new PaymentException(ErrorCode.PAYMENT_FAILED, 
            String.format("Payment failed: %s", reason));
    }
    
    public static PaymentException urlGenerationFailed(String reason) {
        return new PaymentException(ErrorCode.PAYMENT_URL_GENERATION_FAILED, 
            String.format("Failed to generate payment URL: %s", reason));
    }
    
    public static PaymentException webhookVerificationFailed(String reason) {
        return new PaymentException(ErrorCode.PAYMENT_WEBHOOK_VERIFICATION_FAILED, 
            String.format("Payment webhook verification failed: %s", reason));
    }
}