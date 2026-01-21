package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.response.PaymentResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service for handling mock payment operations
 * This is for demo/testing purposes only - in production, this would be handled by actual payment providers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MockPaymentService {
    
    private final OrderService orderService;
    
    /**
     * Prepare payment page data and validate parameters
     * 
     * @param orderNumber The order number
     * @param amount The payment amount
     * @return PaymentResultResponse with page preparation result
     */
    public PaymentResultResponse preparePaymentPage(String orderNumber, BigDecimal amount) {
        log.info("Preparing payment page for order: {} with amount: {}", orderNumber, amount);
        
        // Validate parameters
        if (!validatePaymentParameters(orderNumber, amount)) {
            return PaymentResultResponse.builder()
                    .orderNumber(orderNumber)
                    .message("Invalid payment parameters")
                    .isSuccess(false)
                    .build();
        }
        
        // Log access for monitoring
        logPaymentPageAccess(orderNumber, amount);
        
        return PaymentResultResponse.builder()
                .orderNumber(orderNumber)
                .message("Payment page prepared successfully")
                .isSuccess(true)
                .build();
    }

    /**
     * Process payment success and return result data
     * 
     * @param orderNumber The order number
     * @return PaymentResultResponse with success data
     */
    public PaymentResultResponse processPaymentSuccess(String orderNumber) {
        log.info("Processing payment success for order: {}", orderNumber);
        
        try {
            String trackingToken = orderService.getTrackingTokenByOrderNumber(orderNumber);
            
            return PaymentResultResponse.builder()
                    .orderNumber(orderNumber)
                    .trackingToken(trackingToken)
                    .message("Payment completed successfully!")
                    .isSuccess(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error processing payment success for order: {}", orderNumber, e);
            return PaymentResultResponse.builder()
                    .orderNumber(orderNumber)
                    .message("Error processing payment success: " + e.getMessage())
                    .isSuccess(false)
                    .build();
        }
    }
    
    /**
     * Process payment cancellation and return result data
     * 
     * @param orderNumber The order number
     * @return PaymentResultResponse with cancellation data
     */
    public PaymentResultResponse processPaymentCancellation(String orderNumber) {
        log.info("Processing payment cancellation for order: {}", orderNumber);
        
        return PaymentResultResponse.builder()
                .orderNumber(orderNumber)
                .message("Payment was cancelled.")
                .isSuccess(false)
                .build();
    }
    
    /**
     * Validate mock payment parameters
     * 
     * @param orderNumber The order number
     * @param amount The payment amount
     * @return true if parameters are valid
     */
    public boolean validatePaymentParameters(String orderNumber, BigDecimal amount) {
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            log.warn("Invalid order number: {}", orderNumber);
            return false;
        }
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid payment amount: {}", amount);
            return false;
        }
        
        return true;
    }
    
    /**
     * Log mock payment page access for monitoring
     * 
     * @param orderNumber The order number
     * @param amount The payment amount
     */
    public void logPaymentPageAccess(String orderNumber, BigDecimal amount) {
        log.info("Mock payment page accessed for order: {} with amount: {}", orderNumber, amount);
        
        // In a real system, you might want to:
        // - Track payment page views
        // - Log for fraud detection
        // - Update payment attempt metrics
        // - Validate order exists and is in correct state
    }
}