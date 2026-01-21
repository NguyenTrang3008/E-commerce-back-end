package fs.fresher.SystemE_commerce.controller;

import fs.fresher.SystemE_commerce.dto.request.CheckoutStartRequest;
import fs.fresher.SystemE_commerce.dto.response.CheckoutSessionResponse;
import fs.fresher.SystemE_commerce.service.CheckoutRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {
    
    private final CheckoutRequestService checkoutRequestService;
    
    /**
     * @deprecated Use /api/inventory/reserve instead
     * This endpoint is kept for backward compatibility
     */
    @Deprecated
    @PostMapping("/start")
    public ResponseEntity<CheckoutSessionResponse> startCheckout(@RequestBody CheckoutStartRequest request) {
        CheckoutSessionResponse response = checkoutRequestService.handleStartCheckout(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/sepay-payment-url/{orderNumber}")
    public ResponseEntity<Map<String, String>> createSepayPaymentUrl(@PathVariable String orderNumber) {
        Map<String, String> response = checkoutRequestService.handleCreateSepayPaymentUrl(orderNumber);
        return ResponseEntity.ok(response);
    }
}