package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    @Value("${spring.mail.from:noreply@ecommerce.com}")
    private String fromEmail;
    
    @Value("${spring.mail.enabled:true}")
    private boolean emailEnabled;
    
    public void sendOrderConfirmation(Order order) {
        try {
            String trackingUrl = baseUrl + "/track/" + order.getTrackingToken();
            String emailBody = buildOrderConfirmationEmail(order, trackingUrl);
            
            if (emailEnabled) {
                // Send actual email
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(order.getCustomerEmail());
                message.setSubject("Xác nhận đơn hàng - " + order.getOrderNumber());
                message.setText(emailBody);
                
                mailSender.send(message);
                log.info("Order confirmation email sent successfully to: {}", order.getCustomerEmail());
            } else {
                // Fallback: Log email content
                log.info("=== ORDER CONFIRMATION EMAIL (EMAIL DISABLED) ===");
                log.info("To: {}", order.getCustomerEmail());
                log.info("Subject: Xác nhận đơn hàng - {}", order.getOrderNumber());
                log.info("Email Body:\n{}", emailBody);
                log.info("================================================");
            }
            
        } catch (Exception e) {
            log.error("Failed to send order confirmation email to: {}", order.getCustomerEmail(), e);
            
            // Fallback: Log email content when sending fails
            try {
                String trackingUrl = baseUrl + "/track/" + order.getTrackingToken();
                String emailBody = buildOrderConfirmationEmail(order, trackingUrl);
                log.info("=== ORDER CONFIRMATION EMAIL (FALLBACK) ===");
                log.info("To: {}", order.getCustomerEmail());
                log.info("Subject: Xác nhận đơn hàng - {}", order.getOrderNumber());
                log.info("Email Body:\n{}", emailBody);
                log.info("==========================================");
            } catch (Exception fallbackException) {
                log.error("Failed to log fallback email", fallbackException);
            }
            
            // Don't throw exception - email failure shouldn't break order creation
        }
    }
    
    private String buildOrderConfirmationEmail(Order order, String trackingUrl) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Kính chào ").append(order.getCustomerName()).append(",\n\n");
        sb.append("Cảm ơn bạn đã mua hàng! Đơn hàng của bạn đã được đặt thành công.\n\n");
        
        sb.append("Chi tiết đơn hàng:\n");
        sb.append("Mã đơn hàng: ").append(order.getOrderNumber()).append("\n");
        sb.append("Ngày đặt hàng: ").append(order.getCreatedAt().toLocalDate()).append("\n");
        sb.append("Phương thức thanh toán: ").append(getPaymentMethodText(order.getPaymentMethod())).append("\n");
        sb.append("Tổng tiền: ").append(String.format("%,.0f", order.getTotalAmount())).append(" VNĐ\n\n");
        
        sb.append("Địa chỉ giao hàng:\n");
        sb.append(order.getShippingAddress()).append("\n\n");
        
        sb.append("Sản phẩm đã đặt:\n");
        order.getOrderItems().forEach(item -> {
            sb.append("- ").append(item.getProductName());
            if (item.getSize() != null) sb.append(" (Size: ").append(item.getSize()).append(")");
            if (item.getColor() != null) sb.append(" (Màu: ").append(item.getColor()).append(")");
            sb.append(" - SL: ").append(item.getQuantity());
            sb.append(" - ").append(String.format("%,.0f", item.getTotalPrice())).append(" VNĐ\n");
        });
        
        sb.append("\nBạn có thể theo dõi trạng thái đơn hàng tại đây:\n");
        sb.append(trackingUrl).append("\n\n");
        
        if (order.getPaymentMethod().name().equals("COD")) {
            sb.append("Đơn hàng sẽ được giao đến địa chỉ của bạn và thanh toán khi nhận hàng.\n\n");
        } else {
            sb.append("Vui lòng hoàn tất thanh toán để xử lý đơn hàng.\n\n");
        }
        
        sb.append("Cảm ơn bạn đã mua sắm tại cửa hàng của chúng tôi!\n\n");
        sb.append("Trân trọng,\n");
        sb.append("Đội ngũ E-Commerce");
        
        return sb.toString();
    }
    
    private String getPaymentMethodText(Enum<?> paymentMethod) {
        switch (paymentMethod.name()) {
            case "COD":
                return "Thanh toán khi nhận hàng";
            case "SEPAY":
                return "Thanh toán online qua SePay";
            default:
                return paymentMethod.name();
        }
    }
}