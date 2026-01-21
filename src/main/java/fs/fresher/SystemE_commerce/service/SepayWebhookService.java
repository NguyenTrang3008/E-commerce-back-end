package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.request.SepayWebhookRequest;
import fs.fresher.SystemE_commerce.dto.request.UpdateOrderStatusRequest;
import fs.fresher.SystemE_commerce.entity.Order;
import fs.fresher.SystemE_commerce.enums.OrderStatus;
import fs.fresher.SystemE_commerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SepayWebhookService {
    
    private final OrderRepository orderRepository;
    private final AdminOrderService adminOrderService;
    
    @Value("${sepay.webhook.secret:demo-secret-key}")
    private String webhookSecret;
    
    public Map<String, String> processWebhook(SepayWebhookRequest request) {
        log.info("Processing SePay webhook for order: {}", request.getOrderNumber());
        
        // Validate signature
        if (!validateSignature(request)) {
            log.error("Invalid webhook signature for order: {}", request.getOrderNumber());
            throw new RuntimeException("Invalid webhook signature");
        }
        
        // Find order
        Order order = orderRepository.findByOrderNumber(request.getOrderNumber())
                .orElseThrow(() -> new RuntimeException("Order not found: " + request.getOrderNumber()));
        
        // Process based on payment status
        switch (request.getStatus().toUpperCase()) {
            case "SUCCESS":
                handleSuccessfulPayment(order, request);
                break;
            case "FAILED":
                handleFailedPayment(order, request);
                break;
            case "PENDING":
                handlePendingPayment(order, request);
                break;
            default:
                log.warn("Unknown payment status: {} for order: {}", request.getStatus(), request.getOrderNumber());
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Webhook processed successfully");
        return response;
    }
    
    public Map<String, Object> simulatePayment(String orderNumber, String status, BigDecimal amount) {
        // Generate test webhook data
        String transactionId = "TXN" + System.currentTimeMillis();
        Long timestamp = System.currentTimeMillis();
        
        // If amount is not provided, get it from the actual order
        if (amount == null) {
            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
            amount = order.getTotalAmount();
            log.info("Using order total amount: {} for order: {}", amount, orderNumber);
        }
        
        // Generate signature
        String signature = generateSignature(
            transactionId, orderNumber, amount.toString(), status, timestamp
        );
        
        // Create webhook request
        SepayWebhookRequest webhookRequest = new SepayWebhookRequest();
        webhookRequest.setTransactionId(transactionId);
        webhookRequest.setOrderNumber(orderNumber);
        webhookRequest.setAmount(amount);
        webhookRequest.setStatus(status);
        webhookRequest.setPaymentMethod("BANK_TRANSFER");
        webhookRequest.setSignature(signature);
        webhookRequest.setTimestamp(timestamp);
        webhookRequest.setDescription("Simulated payment for testing");
        
        // Process webhook
        processWebhook(webhookRequest);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Payment simulation completed");
        response.put("transactionId", transactionId);
        response.put("orderNumber", orderNumber);
        response.put("paymentStatus", status);
        
        return response;
    }
    
    private void handleSuccessfulPayment(Order order, SepayWebhookRequest request) {
        log.info("Payment successful for order: {}", order.getOrderNumber());
        
        if (order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            // Update order to PAID status
            UpdateOrderStatusRequest updateRequest = new UpdateOrderStatusRequest(
                OrderStatus.PAID,
                "Payment confirmed via SePay webhook - Transaction ID: " + request.getTransactionId()
            );
            
            adminOrderService.updateOrderStatus(order.getId(), updateRequest, "SYSTEM");
            
            log.info("Order {} status updated to PAID", order.getOrderNumber());
        } else {
            log.warn("Order {} is not in AWAITING_PAYMENT status. Current status: {}", 
                    order.getOrderNumber(), order.getStatus());
        }
    }
    
    private void handleFailedPayment(Order order, SepayWebhookRequest request) {
        log.info("Payment failed for order: {}", order.getOrderNumber());
        
        if (order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            // Cancel the order
            UpdateOrderStatusRequest updateRequest = new UpdateOrderStatusRequest(
                OrderStatus.CANCELLED,
                "Payment failed via SePay webhook - Transaction ID: " + request.getTransactionId()
            );
            
            adminOrderService.updateOrderStatus(order.getId(), updateRequest, "SYSTEM");
            
            log.info("Order {} cancelled due to payment failure", order.getOrderNumber());
        }
    }
    
    private void handlePendingPayment(Order order, SepayWebhookRequest request) {
        log.info("Payment pending for order: {}", order.getOrderNumber());
        // Keep order in AWAITING_PAYMENT status
        // Could add additional logging or notifications here
    }
    
    private boolean validateSignature(SepayWebhookRequest request) {
        try {
            // Create signature payload
            String payload = request.getTransactionId() + 
                           request.getOrderNumber() + 
                           request.getAmount() + 
                           request.getStatus() + 
                           request.getTimestamp().toString();
            
            // Generate expected signature
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);
            
            return expectedSignature.equals(request.getSignature());
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error validating webhook signature", e);
            return false;
        }
    }
    
    // Method to generate signature for testing
    public String generateSignature(String transactionId, String orderNumber, 
                                  String amount, String status, Long timestamp) {
        try {
            String payload = transactionId + orderNumber + amount + status + timestamp.toString();
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating signature", e);
            return null;
        }
    }
}