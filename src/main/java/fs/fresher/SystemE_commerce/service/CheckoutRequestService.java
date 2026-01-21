package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.request.CheckoutStartRequest;
import fs.fresher.SystemE_commerce.dto.response.CheckoutSessionResponse;
import fs.fresher.SystemE_commerce.entity.Order;
import fs.fresher.SystemE_commerce.repository.OrderRepository;
import fs.fresher.SystemE_commerce.validator.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutRequestService {
    
    private final CheckoutService checkoutService;
    private final SepayPaymentService sepayPaymentService;
    private final OrderRepository orderRepository;
    private final ValidationService validationService;
    
    /**
     * Handle start checkout request
     */
    public CheckoutSessionResponse handleStartCheckout(CheckoutStartRequest request) {
        validationService.validateCartId(request.getCartId(), true);
        
        log.info("Starting checkout for cart: {}", request.getCartId());
        return checkoutService.startCheckout(request);
    }
    
    /**
     * Handle create SePay payment URL request
     */
    public Map<String, String> handleCreateSepayPaymentUrl(String orderNumber) {
        validationService.validateOrderNumber(orderNumber);
        
        try {
            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
            
            String paymentUrl = sepayPaymentService.createPaymentUrl(order);
            
            Map<String, String> response = new HashMap<>();
            response.put("paymentUrl", paymentUrl);
            response.put("orderNumber", orderNumber);
            
            log.info("Created SePay payment URL for order: {}", orderNumber);
            return response;
            
        } catch (Exception e) {
            log.error("Failed to create SePay payment URL for order {}: {}", orderNumber, e.getMessage());
            
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("orderNumber", orderNumber);
            return response;
        }
    }
}