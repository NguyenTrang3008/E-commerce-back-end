package fs.fresher.SystemE_commerce.controller;

import fs.fresher.SystemE_commerce.dto.request.SepayWebhookRequest;
import fs.fresher.SystemE_commerce.service.SepayWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/sepay")
@RequiredArgsConstructor
@Slf4j
public class SepayWebhookController {
    
    private final SepayWebhookService sepayWebhookService;
    
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(@RequestBody SepayWebhookRequest request) {
        log.info("Received SePay webhook for order: {}", request.getOrderNumber());
        Map<String, String> response = sepayWebhookService.processWebhook(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/simulate-payment")
    public ResponseEntity<Map<String, Object>> simulatePayment(
            @RequestParam String orderNumber,
            @RequestParam String status,
            @RequestParam(required = false) BigDecimal amount) {
        
        Map<String, Object> response = sepayWebhookService.simulatePayment(orderNumber, status, amount);
        return ResponseEntity.ok(response);
    }
}