package fs.fresher.SystemE_commerce.dto.response;

import fs.fresher.SystemE_commerce.enums.OrderStatus;
import fs.fresher.SystemE_commerce.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingResponse {
    private String orderNumber;
    private OrderStatus currentStatus;
    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;
    private String customerName;
    private String shippingAddress;
    private List<OrderItemSummary> items;
    private List<StatusHistoryResponse> statusHistory;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemSummary {
        private String productName;
        private String sku;
        private String size;
        private String color;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistoryResponse {
        private OrderStatus status;
        private String note;
        private LocalDateTime timestamp;
    }
}