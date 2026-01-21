package fs.fresher.SystemE_commerce.dto.response;

import fs.fresher.SystemE_commerce.enums.OrderStatus;
import fs.fresher.SystemE_commerce.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderListResponse {
    private Long orderId;
    private String orderNumber;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer totalItems;
}