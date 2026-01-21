package fs.fresher.SystemE_commerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for payment result operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultResponse {
    
    /**
     * Order number
     */
    private String orderNumber;
    
    /**
     * Tracking token for order tracking
     */
    private String trackingToken;
    
    /**
     * Result message
     */
    private String message;
    
    /**
     * Whether the payment was successful
     */
    private boolean isSuccess;
}