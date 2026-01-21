package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.response.PaymentResultResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MockPaymentServiceTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private MockPaymentService mockPaymentService;

    private String validOrderNumber;
    private String validTrackingToken;

    @BeforeEach
    void setUp() {
        validOrderNumber = "ORD20240121140000001";
        validTrackingToken = "tracking-token-123";
    }

    @Test
    void processPaymentSuccess_WithValidOrder_ShouldReturnSuccessResponse() {
        // Given
        when(orderService.getTrackingTokenByOrderNumber(validOrderNumber))
                .thenReturn(validTrackingToken);

        // When
        PaymentResultResponse result = mockPaymentService.processPaymentSuccess(validOrderNumber);

        // Then
        assertNotNull(result);
        assertEquals(validOrderNumber, result.getOrderNumber());
        assertEquals(validTrackingToken, result.getTrackingToken());
        assertEquals("Payment completed successfully!", result.getMessage());
        assertTrue(result.isSuccess());
        
        verify(orderService).getTrackingTokenByOrderNumber(validOrderNumber);
    }

    @Test
    void processPaymentSuccess_WithOrderServiceException_ShouldReturnErrorResponse() {
        // Given
        String errorMessage = "Order not found";
        when(orderService.getTrackingTokenByOrderNumber(validOrderNumber))
                .thenThrow(new RuntimeException(errorMessage));

        // When
        PaymentResultResponse result = mockPaymentService.processPaymentSuccess(validOrderNumber);

        // Then
        assertNotNull(result);
        assertEquals(validOrderNumber, result.getOrderNumber());
        assertNull(result.getTrackingToken());
        assertTrue(result.getMessage().contains("Error processing payment success"));
        assertTrue(result.getMessage().contains(errorMessage));
        assertFalse(result.isSuccess());
        
        verify(orderService).getTrackingTokenByOrderNumber(validOrderNumber);
    }

    @Test
    void processPaymentCancellation_ShouldReturnCancellationResponse() {
        // When
        PaymentResultResponse result = mockPaymentService.processPaymentCancellation(validOrderNumber);

        // Then
        assertNotNull(result);
        assertEquals(validOrderNumber, result.getOrderNumber());
        assertNull(result.getTrackingToken());
        assertEquals("Payment was cancelled.", result.getMessage());
        assertFalse(result.isSuccess());
        
        verifyNoInteractions(orderService);
    }

    @Test
    void validatePaymentParameters_WithValidParameters_ShouldReturnTrue() {
        // Given
        BigDecimal validAmount = new BigDecimal("100.00");

        // When
        boolean result = mockPaymentService.validatePaymentParameters(validOrderNumber, validAmount);

        // Then
        assertTrue(result);
    }

    @Test
    void validatePaymentParameters_WithNullOrderNumber_ShouldReturnFalse() {
        // Given
        BigDecimal validAmount = new BigDecimal("100.00");

        // When
        boolean result = mockPaymentService.validatePaymentParameters(null, validAmount);

        // Then
        assertFalse(result);
    }

    @Test
    void validatePaymentParameters_WithEmptyOrderNumber_ShouldReturnFalse() {
        // Given
        BigDecimal validAmount = new BigDecimal("100.00");

        // When
        boolean result = mockPaymentService.validatePaymentParameters("", validAmount);

        // Then
        assertFalse(result);
    }

    @Test
    void validatePaymentParameters_WithBlankOrderNumber_ShouldReturnFalse() {
        // Given
        BigDecimal validAmount = new BigDecimal("100.00");

        // When
        boolean result = mockPaymentService.validatePaymentParameters("   ", validAmount);

        // Then
        assertFalse(result);
    }

    @Test
    void validatePaymentParameters_WithNullAmount_ShouldReturnFalse() {
        // When
        boolean result = mockPaymentService.validatePaymentParameters(validOrderNumber, null);

        // Then
        assertFalse(result);
    }

    @Test
    void validatePaymentParameters_WithZeroAmount_ShouldReturnFalse() {
        // Given
        BigDecimal zeroAmount = BigDecimal.ZERO;

        // When
        boolean result = mockPaymentService.validatePaymentParameters(validOrderNumber, zeroAmount);

        // Then
        assertFalse(result);
    }

    @Test
    void validatePaymentParameters_WithNegativeAmount_ShouldReturnFalse() {
        // Given
        BigDecimal negativeAmount = new BigDecimal("-10.00");

        // When
        boolean result = mockPaymentService.validatePaymentParameters(validOrderNumber, negativeAmount);

        // Then
        assertFalse(result);
    }

    @Test
    void validatePaymentParameters_WithBothInvalidParameters_ShouldReturnFalse() {
        // When
        boolean result = mockPaymentService.validatePaymentParameters(null, null);

        // Then
        assertFalse(result);
    }

    @Test
    void logPaymentPageAccess_ShouldNotThrowException() {
        // Given
        BigDecimal amount = new BigDecimal("250.00");

        // When & Then - Should not throw any exception
        assertDoesNotThrow(() -> {
            mockPaymentService.logPaymentPageAccess(validOrderNumber, amount);
        });
    }

    @Test
    void logPaymentPageAccess_WithNullParameters_ShouldNotThrowException() {
        // When & Then - Should not throw any exception
        assertDoesNotThrow(() -> {
            mockPaymentService.logPaymentPageAccess(null, null);
        });
    }

    @Test
    void processPaymentSuccess_WithNullOrderNumber_ShouldHandleGracefully() {
        // Given
        when(orderService.getTrackingTokenByOrderNumber(null))
                .thenThrow(new RuntimeException("Order number cannot be null"));

        // When
        PaymentResultResponse result = mockPaymentService.processPaymentSuccess(null);

        // Then
        assertNotNull(result);
        assertNull(result.getOrderNumber());
        assertNull(result.getTrackingToken());
        assertTrue(result.getMessage().contains("Error processing payment success"));
        assertFalse(result.isSuccess());
    }

    @Test
    void processPaymentCancellation_WithNullOrderNumber_ShouldHandleGracefully() {
        // When
        PaymentResultResponse result = mockPaymentService.processPaymentCancellation(null);

        // Then
        assertNotNull(result);
        assertNull(result.getOrderNumber());
        assertEquals("Payment was cancelled.", result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    void validatePaymentParameters_WithVeryLargeAmount_ShouldReturnTrue() {
        // Given
        BigDecimal largeAmount = new BigDecimal("999999999.99");

        // When
        boolean result = mockPaymentService.validatePaymentParameters(validOrderNumber, largeAmount);

        // Then
        assertTrue(result);
    }

    @Test
    void validatePaymentParameters_WithVerySmallPositiveAmount_ShouldReturnTrue() {
        // Given
        BigDecimal smallAmount = new BigDecimal("0.01");

        // When
        boolean result = mockPaymentService.validatePaymentParameters(validOrderNumber, smallAmount);

        // Then
        assertTrue(result);
    }

    // Tests for preparePaymentPage method
    @Test
    void preparePaymentPage_WithValidParameters_ShouldReturnSuccessResponse() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");

        // When
        PaymentResultResponse result = mockPaymentService.preparePaymentPage(validOrderNumber, amount);

        // Then
        assertNotNull(result);
        assertEquals(validOrderNumber, result.getOrderNumber());
        assertEquals("Payment page prepared successfully", result.getMessage());
        assertTrue(result.isSuccess());
    }

    @Test
    void preparePaymentPage_WithInvalidOrderNumber_ShouldReturnFailureResponse() {
        // Given
        String invalidOrderNumber = "";
        BigDecimal amount = new BigDecimal("100.00");

        // When
        PaymentResultResponse result = mockPaymentService.preparePaymentPage(invalidOrderNumber, amount);

        // Then
        assertNotNull(result);
        assertEquals(invalidOrderNumber, result.getOrderNumber());
        assertEquals("Invalid payment parameters", result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    void preparePaymentPage_WithInvalidAmount_ShouldReturnFailureResponse() {
        // Given
        BigDecimal invalidAmount = BigDecimal.ZERO;

        // When
        PaymentResultResponse result = mockPaymentService.preparePaymentPage(validOrderNumber, invalidAmount);

        // Then
        assertNotNull(result);
        assertEquals(validOrderNumber, result.getOrderNumber());
        assertEquals("Invalid payment parameters", result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    void preparePaymentPage_WithNullParameters_ShouldReturnFailureResponse() {
        // When
        PaymentResultResponse result = mockPaymentService.preparePaymentPage(null, null);

        // Then
        assertNotNull(result);
        assertNull(result.getOrderNumber());
        assertEquals("Invalid payment parameters", result.getMessage());
        assertFalse(result.isSuccess());
    }
}