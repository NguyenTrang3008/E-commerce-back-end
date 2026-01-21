package fs.fresher.SystemE_commerce.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SepayWebhookRequest {
    private String transactionId;
    private String orderNumber;
    private BigDecimal amount;
    private String status; // SUCCESS, FAILED, PENDING
    private String paymentMethod;
    private String signature;
    private Long timestamp;
    private String description;
}