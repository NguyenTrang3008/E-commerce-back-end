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
public class AdminOrderDetailResponse {
    private Long orderId;
    private String orderNumber;
    private String trackingToken;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Customer info
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String shippingAddress;
    private String note;
    
    // Order items
    private List<AdminOrderItemResponse> items;
    
    // Status history
    private List<AdminStatusHistoryResponse> statusHistory;
    
    // Available status transitions
    private List<OrderStatus> availableTransitions;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminOrderItemResponse {
        private String productName;
        private String sku;
        private String size;
        private String color;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminStatusHistoryResponse {
        private OrderStatus previousStatus;
        private OrderStatus status;
        private String note;
        private String changedBy;
        private LocalDateTime timestamp;
    }
}