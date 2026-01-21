package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SepayPaymentService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${sepay.api.url}")
    private String sepayApiUrl;
    
    @Value("${sepay.merchant.id}")
    private String merchantId;
    
    @Value("${app.base-url}")
    private String baseUrl;
    
    public String createPaymentUrl(Order order) {
        try {
            // Tạo URL thanh toán SePay
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("amount", order.getTotalAmount());
            paymentRequest.put("order_id", order.getOrderNumber());
            paymentRequest.put("description", "Thanh toán đơn hàng " + order.getOrderNumber());
            paymentRequest.put("return_url", baseUrl + "/api/sepay/return");
            paymentRequest.put("cancel_url", baseUrl + "/api/sepay/cancel");
            paymentRequest.put("notify_url", baseUrl + "/api/sepay/webhook");
            
            // Trong thực tế, bạn sẽ gọi API SePay để tạo payment URL
            // Ở đây tôi tạo URL giả lập cho demo
            String paymentUrl = generateMockPaymentUrl(order);
            
            log.info("Created payment URL for order {}: {}", order.getOrderNumber(), paymentUrl);
            return paymentUrl;
            
        } catch (Exception e) {
            log.error("Error creating SePay payment URL for order: {}", order.getOrderNumber(), e);
            throw new RuntimeException("Failed to create payment URL", e);
        }
    }
    
    private String generateMockPaymentUrl(Order order) {
        // URL giả lập cho demo - trong thực tế sẽ là URL từ SePay
        return String.format("%s/sepay-mock-payment?order=%s&amount=%s", 
                baseUrl, order.getOrderNumber(), order.getTotalAmount());
    }
    
    public boolean verifyPayment(String transactionId, String orderNumber, BigDecimal amount) {
        try {
            // Trong thực tế, bạn sẽ gọi API SePay để verify transaction
            log.info("Verifying payment - Transaction: {}, Order: {}, Amount: {}", 
                    transactionId, orderNumber, amount);
            
            // Mock verification - luôn return true cho demo
            return true;
            
        } catch (Exception e) {
            log.error("Error verifying payment", e);
            return false;
        }
    }
}