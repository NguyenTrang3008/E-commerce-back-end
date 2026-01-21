package fs.fresher.SystemE_commerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSessionResponse {
    private String checkoutToken;
    private String cartId;
    private LocalDateTime expiresAt;
    private List<ReservedItemResponse> reservedItems;
    private BigDecimal totalAmount;
    private Integer totalItems;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservedItemResponse {
        private Long skuId;
        private String sku;
        private String productName;
        private String size;
        private String color;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;
    }
}