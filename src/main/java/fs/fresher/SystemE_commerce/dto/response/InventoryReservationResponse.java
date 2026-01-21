package fs.fresher.SystemE_commerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class InventoryReservationResponse {
    
    private String reservationToken;
    private String cartId;
    private LocalDateTime expiresAt;
    private List<ReservedItemResponse> reservedItems;
    private BigDecimal totalAmount;
    private Integer totalItems;
    
    @Data
    @AllArgsConstructor
    public static class ReservedItemResponse {
        private Long variantId;
        private String sku;
        private String productName;
        private String size;
        private String color;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;
    }
}