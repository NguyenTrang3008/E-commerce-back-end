package fs.fresher.SystemE_commerce.exception;

import lombok.Getter;

/**
 * Exception thrown when there is insufficient stock for a product variant
 */
@Getter
public class InsufficientStockException extends BusinessException {
    private final Long variantId;
    private final int requestedQuantity;
    private final int availableQuantity;
    
    public InsufficientStockException(Long variantId, int requestedQuantity, int availableQuantity) {
        super(ErrorCode.INSUFFICIENT_STOCK, 
              String.format("Insufficient stock for variant %d. Requested: %d, Available: %d", 
                           variantId, requestedQuantity, availableQuantity));
        this.variantId = variantId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }
}