package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.request.SepayWebhookRequest;
import fs.fresher.SystemE_commerce.dto.request.UpdateOrderStatusRequest;
import fs.fresher.SystemE_commerce.entity.Order;
import fs.fresher.SystemE_commerce.enums.OrderStatus;
import fs.fresher.SystemE_commerce.enums.PaymentMethod;
import fs.fresher.SystemE_commerce.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SepayWebhookServiceTest {

    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private AdminOrderService adminOrderService;

    @InjectMocks
    private SepayWebhookService sepayWebhookService;

    private Order mockOrder;
    private SepayWebhookRequest successWebhookRequest;
    private SepayWebhookRequest failedWebhookRequest;
    private SepayWebhookRequest pendingWebhookRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sepayWebhookService, "webhookSecret", "demo-secret-key");
        setupTestData();
    }

    private void setupTestData() {
        mockOrder = new Order();
        mockOrder.setId(1L);
        mockOrder.setOrderNumber("ORD20240101120000001");
        mockOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
        mockOrder.setPaymentMethod(PaymentMethod.SEPAY);
        mockOrder.setTotalAmount(new BigDecimal("250.00"));
        mockOrder.setCreatedAt(LocalDateTime.now());

        // Create webhook requests with valid signatures
        Long timestamp = System.currentTimeMillis();
        
        successWebhookRequest = createWebhookRequest("TXN123456789", "ORD20240101120000001", 
                new BigDecimal("250.00"), "SUCCESS", timestamp);
        
        failedWebhookRequest = createWebhookRequest("TXN123456790", "ORD20240101120000001", 
                new BigDecimal("250.00"), "FAILED", timestamp);
        
        pendingWebhookRequest = createWebhookRequest("TXN123456791", "ORD20240101120000001", 
                new BigDecimal("250.00"), "PENDING", timestamp);
    }

    private SepayWebhookRequest createWebhookRequest(String transactionId, String orderNumber, 
                                                   BigDecimal amount, String status, Long timestamp) {
        SepayWebhookRequest request = new SepayWebhookRequest();
        request.setTransactionId(transactionId);
        request.setOrderNumber(orderNumber);
        request.setAmount(amount);
        request.setStatus(status);
        request.setTimestamp(timestamp);
        request.setPaymentMethod("BANK_TRANSFER");
        request.setDescription("Test payment");
        
        // Generate valid signature
        String signature = sepayWebhookService.generateSignature(
                transactionId, orderNumber, amount.toString(), status, timestamp);
        request.setSignature(signature);
        
        return request;
    }

    @Test
    void processWebhook_WithSuccessfulPayment_ShouldUpdateOrderToPaid() {
        // Given
        when(orderRepository.findByOrderNumber("ORD20240101120000001"))
                .thenReturn(Optional.of(mockOrder));

        // When
        sepayWebhookService.processWebhook(successWebhookRequest);

        // Then
        verify(adminOrderService).updateOrderStatus(
                eq(1L), 
                argThat(request -> request.getStatus() == OrderStatus.PAID &&
                        request.getNote().contains("Payment confirmed via SePay webhook")),
                eq("SYSTEM")
        );
    }

    @Test
    void processWebhook_WithFailedPayment_ShouldCancelOrder() {
        // Given
        when(orderRepository.findByOrderNumber("ORD20240101120000001"))
                .thenReturn(Optional.of(mockOrder));

        // When
        sepayWebhookService.processWebhook(failedWebhookRequest);

        // Then
        verify(adminOrderService).updateOrderStatus(
                eq(1L), 
                argThat(request -> request.getStatus() == OrderStatus.CANCELLED &&
                        request.getNote().contains("Payment failed via SePay webhook")),
                eq("SYSTEM")
        );
    }

    @Test
    void processWebhook_WithPendingPayment_ShouldNotUpdateOrder() {
        // Given
        when(orderRepository.findByOrderNumber("ORD20240101120000001"))
                .thenReturn(Optional.of(mockOrder));

        // When
        sepayWebhookService.processWebhook(pendingWebhookRequest);

        // Then
        verify(adminOrderService, never()).updateOrderStatus(anyLong(), any(), anyString());
    }

    @Test
    void processWebhook_WithInvalidSignature_ShouldThrowException() {
        // Given
        successWebhookRequest.setSignature("invalid-signature");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> sepayWebhookService.processWebhook(successWebhookRequest));
        assertEquals("Invalid webhook signature", exception.getMessage());
        
        verify(orderRepository, never()).findByOrderNumber(anyString());
        verify(adminOrderService, never()).updateOrderStatus(anyLong(), any(), anyString());
    }

    @Test
    void processWebhook_WithNonExistentOrder_ShouldThrowException() {
        // Given
        when(orderRepository.findByOrderNumber("ORD20240101120000001"))
                .thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> sepayWebhookService.processWebhook(successWebhookRequest));
        assertEquals("Order not found: ORD20240101120000001", exception.getMessage());
        
        verify(adminOrderService, never()).updateOrderStatus(anyLong(), any(), anyString());
    }

    @Test
    void processWebhook_WithOrderNotInAwaitingPaymentStatus_ShouldNotUpdateOrder() {
        // Given
        mockOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findByOrderNumber("ORD20240101120000001"))
                .thenReturn(Optional.of(mockOrder));

        // When
        sepayWebhookService.processWebhook(successWebhookRequest);

        // Then
        verify(adminOrderService, never()).updateOrderStatus(anyLong(), any(), anyString());
    }

    @Test
    void processWebhook_WithUnknownStatus_ShouldNotUpdateOrder() {
        // Given
        Long timestamp = System.currentTimeMillis();
        SepayWebhookRequest unknownStatusRequest = createWebhookRequest(
                "TXN123456792", "ORD20240101120000001", 
                new BigDecimal("250.00"), "UNKNOWN", timestamp);
        
        when(orderRepository.findByOrderNumber("ORD20240101120000001"))
                .thenReturn(Optional.of(mockOrder));

        // When
        sepayWebhookService.processWebhook(unknownStatusRequest);

        // Then
        verify(adminOrderService, never()).updateOrderStatus(anyLong(), any(), anyString());
    }

    @Test
    void generateSignature_WithValidData_ShouldReturnConsistentSignature() {
        // Given
        String transactionId = "TXN123456789";
        String orderNumber = "ORD20240101120000001";
        String amount = "250.00";
        String status = "SUCCESS";
        Long timestamp = 1640995200000L; // Fixed timestamp for consistency

        // When
        String signature1 = sepayWebhookService.generateSignature(transactionId, orderNumber, amount, status, timestamp);
        String signature2 = sepayWebhookService.generateSignature(transactionId, orderNumber, amount, status, timestamp);

        // Then
        assertNotNull(signature1);
        assertNotNull(signature2);
        assertEquals(signature1, signature2);
    }

    @Test
    void generateSignature_WithDifferentData_ShouldReturnDifferentSignatures() {
        // Given
        Long timestamp = System.currentTimeMillis();

        // When
        String signature1 = sepayWebhookService.generateSignature("TXN1", "ORD1", "100.00", "SUCCESS", timestamp);
        String signature2 = sepayWebhookService.generateSignature("TXN2", "ORD1", "100.00", "SUCCESS", timestamp);

        // Then
        assertNotNull(signature1);
        assertNotNull(signature2);
        assertNotEquals(signature1, signature2);
    }

    @Test
    void processWebhook_WithCaseInsensitiveStatus_ShouldWork() {
        // Given
        Long timestamp = System.currentTimeMillis();
        SepayWebhookRequest lowercaseRequest = createWebhookRequest(
                "TXN123456793", "ORD20240101120000001", 
                new BigDecimal("250.00"), "success", timestamp);
        
        when(orderRepository.findByOrderNumber("ORD20240101120000001"))
                .thenReturn(Optional.of(mockOrder));

        // When
        sepayWebhookService.processWebhook(lowercaseRequest);

        // Then
        verify(adminOrderService).updateOrderStatus(
                eq(1L), 
                argThat(request -> request.getStatus() == OrderStatus.PAID),
                eq("SYSTEM")
        );
    }

    @Test
    void processWebhook_WithFailedPaymentOnNonAwaitingOrder_ShouldNotUpdateOrder() {
        // Given
        mockOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findByOrderNumber("ORD20240101120000001"))
                .thenReturn(Optional.of(mockOrder));

        // When
        sepayWebhookService.processWebhook(failedWebhookRequest);

        // Then
        verify(adminOrderService, never()).updateOrderStatus(anyLong(), any(), anyString());
    }

    @Test
    void generateSignature_WithNullValues_ShouldHandleGracefully() {
        // When & Then
        assertDoesNotThrow(() -> {
            String signature = sepayWebhookService.generateSignature(null, null, null, null, null);
            // The method should handle nulls gracefully, though the signature may not be meaningful
        });
    }

    @Test
    void simulatePayment_WithoutAmount_ShouldUseOrderTotalAmount() {
        // Given
        when(orderRepository.findByOrderNumber("ORD20240101120000001"))
                .thenReturn(Optional.of(mockOrder));

        // When
        Map<String, Object> result = sepayWebhookService.simulatePayment("ORD20240101120000001", "SUCCESS", null);

        // Then
        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("Payment simulation completed", result.get("message"));
        assertEquals("ORD20240101120000001", result.get("orderNumber"));
        assertEquals("SUCCESS", result.get("paymentStatus"));
        assertNotNull(result.get("transactionId"));
        
        // Verify that the order was found and webhook was processed
        verify(orderRepository, times(2)).findByOrderNumber("ORD20240101120000001"); // Once for amount, once for processing
        verify(adminOrderService).updateOrderStatus(
                eq(1L), 
                argThat(request -> request.getStatus() == OrderStatus.PAID),
                eq("SYSTEM")
        );
    }

    @Test
    void simulatePayment_WithSpecificAmount_ShouldUseProvidedAmount() {
        // Given
        BigDecimal customAmount = new BigDecimal("500.00");
        when(orderRepository.findByOrderNumber("ORD20240101120000001"))
                .thenReturn(Optional.of(mockOrder));

        // When
        Map<String, Object> result = sepayWebhookService.simulatePayment("ORD20240101120000001", "SUCCESS", customAmount);

        // Then
        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("Payment simulation completed", result.get("message"));
        assertEquals("ORD20240101120000001", result.get("orderNumber"));
        assertEquals("SUCCESS", result.get("paymentStatus"));
        assertNotNull(result.get("transactionId"));
        
        // Verify that the order was found only once for processing (not for getting amount)
        verify(orderRepository, times(1)).findByOrderNumber("ORD20240101120000001");
        verify(adminOrderService).updateOrderStatus(
                eq(1L), 
                argThat(request -> request.getStatus() == OrderStatus.PAID),
                eq("SYSTEM")
        );
    }

    @Test
    void simulatePayment_WithFailedStatus_ShouldCancelOrder() {
        // Given
        when(orderRepository.findByOrderNumber("ORD20240101120000001"))
                .thenReturn(Optional.of(mockOrder));

        // When
        Map<String, Object> result = sepayWebhookService.simulatePayment("ORD20240101120000001", "FAILED", null);

        // Then
        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("Payment simulation completed", result.get("message"));
        assertEquals("ORD20240101120000001", result.get("orderNumber"));
        assertEquals("FAILED", result.get("paymentStatus"));
        assertNotNull(result.get("transactionId"));
        
        // Verify that order was cancelled
        verify(adminOrderService).updateOrderStatus(
                eq(1L), 
                argThat(request -> request.getStatus() == OrderStatus.CANCELLED),
                eq("SYSTEM")
        );
    }

    @Test
    void simulatePayment_WithPendingStatus_ShouldNotUpdateOrder() {
        // Given
        when(orderRepository.findByOrderNumber("ORD20240101120000001"))
                .thenReturn(Optional.of(mockOrder));

        // When
        Map<String, Object> result = sepayWebhookService.simulatePayment("ORD20240101120000001", "PENDING", null);

        // Then
        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals("Payment simulation completed", result.get("message"));
        assertEquals("ORD20240101120000001", result.get("orderNumber"));
        assertEquals("PENDING", result.get("paymentStatus"));
        assertNotNull(result.get("transactionId"));
        
        // Verify that order status was not updated for pending payment
        verify(adminOrderService, never()).updateOrderStatus(anyLong(), any(), anyString());
    }

    @Test
    void simulatePayment_WithNonExistentOrder_ShouldThrowException() {
        // Given
        when(orderRepository.findByOrderNumber("NON_EXISTENT_ORDER"))
                .thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> sepayWebhookService.simulatePayment("NON_EXISTENT_ORDER", "SUCCESS", null));
        
        assertEquals("Order not found: NON_EXISTENT_ORDER", exception.getMessage());
        verify(adminOrderService, never()).updateOrderStatus(anyLong(), any(), anyString());
    }

    @Test
    void simulatePayment_ShouldGenerateValidSignature() {
        // Given
        when(orderRepository.findByOrderNumber("ORD20240101120000001"))
                .thenReturn(Optional.of(mockOrder));

        // When
        Map<String, Object> result = sepayWebhookService.simulatePayment("ORD20240101120000001", "SUCCESS", null);

        // Then
        assertNotNull(result);
        String transactionId = (String) result.get("transactionId");
        assertNotNull(transactionId);
        assertTrue(transactionId.startsWith("TXN"));
        
        // Verify that a valid signature would be generated (we can't test the exact signature due to timestamp)
        String testSignature = sepayWebhookService.generateSignature(
                transactionId, "ORD20240101120000001", "250.00", "SUCCESS", System.currentTimeMillis());
        assertNotNull(testSignature);
        assertFalse(testSignature.isEmpty());
    }

    @Test
    void simulatePayment_WithOrderNotInAwaitingPaymentStatus_ShouldStillProcessWebhook() {
        // Given
        mockOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findByOrderNumber("ORD20240101120000001"))
                .thenReturn(Optional.of(mockOrder));

        // When
        Map<String, Object> result = sepayWebhookService.simulatePayment("ORD20240101120000001", "SUCCESS", null);

        // Then
        assertNotNull(result);
        assertEquals("success", result.get("status"));
        
        // Verify that webhook was processed but order status was not updated (due to current status)
        verify(orderRepository, times(2)).findByOrderNumber("ORD20240101120000001");
        verify(adminOrderService, never()).updateOrderStatus(anyLong(), any(), anyString());
    }

    @Test
    void simulatePayment_ShouldCreateWebhookRequestWithCorrectData() {
        // Given
        BigDecimal customAmount = new BigDecimal("300.00");
        when(orderRepository.findByOrderNumber("ORD20240101120000001"))
                .thenReturn(Optional.of(mockOrder));

        // When
        Map<String, Object> result = sepayWebhookService.simulatePayment("ORD20240101120000001", "SUCCESS", customAmount);

        // Then
        assertNotNull(result);
        assertEquals("ORD20240101120000001", result.get("orderNumber"));
        assertEquals("SUCCESS", result.get("paymentStatus"));
        
        String transactionId = (String) result.get("transactionId");
        assertNotNull(transactionId);
        assertTrue(transactionId.startsWith("TXN"));
        
        // Verify the webhook processing was called
        verify(adminOrderService).updateOrderStatus(
                eq(1L), 
                argThat(request -> 
                    request.getStatus() == OrderStatus.PAID &&
                    request.getNote().contains("Payment confirmed via SePay webhook") &&
                    request.getNote().contains(transactionId)
                ),
                eq("SYSTEM")
        );
    }
}