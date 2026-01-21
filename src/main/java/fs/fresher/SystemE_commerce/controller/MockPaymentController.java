package fs.fresher.SystemE_commerce.controller;

import fs.fresher.SystemE_commerce.dto.response.PaymentResultResponse;
import fs.fresher.SystemE_commerce.service.MockPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MockPaymentController {

    private final MockPaymentService mockPaymentService;

    /**
     * Mock payment page for SePay simulation
     * This is for demo purposes only - in production, this would be handled by the actual payment provider
     */
    @GetMapping("/sepay-mock-payment")
    public String mockPaymentPage(
            @RequestParam("order") String orderNumber,
            @RequestParam("amount") BigDecimal amount,
            Model model) {

        // Validate parameters using service
        if (!mockPaymentService.validatePaymentParameters(orderNumber, amount)) {
            model.addAttribute("error", "Invalid payment parameters");
            return "error";
        }

        // Log access through service
        mockPaymentService.logPaymentPageAccess(orderNumber, amount);

        model.addAttribute("orderNumber", orderNumber);
        model.addAttribute("amount", amount);

        return "mock-payment";
    }

    /**
     * Payment success page
     */
    @GetMapping("/payment-success")
    public String paymentSuccess(@RequestParam("order") String orderNumber, Model model) {
        log.info("Payment success page requested for order: {}", orderNumber);

        // Process payment success through service
        PaymentResultResponse result = mockPaymentService.processPaymentSuccess(orderNumber);

        model.addAttribute("orderNumber", result.getOrderNumber());
        model.addAttribute("trackingToken", result.getTrackingToken());
        model.addAttribute("message", result.getMessage());
        model.addAttribute("isSuccess", result.isSuccess());

        return "payment-result";
    }

    /**
     * Payment cancelled page
     */
    @GetMapping("/payment-cancelled")
    public String paymentCancelled(@RequestParam("order") String orderNumber, Model model) {
        log.info("Payment cancelled page requested for order: {}", orderNumber);

        // Process payment cancellation through service
        PaymentResultResponse result = mockPaymentService.processPaymentCancellation(orderNumber);

        model.addAttribute("orderNumber", result.getOrderNumber());
        model.addAttribute("message", result.getMessage());
        model.addAttribute("isSuccess", result.isSuccess());

        return "payment-result";
    }
}