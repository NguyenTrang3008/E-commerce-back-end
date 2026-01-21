package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.entity.Order;
import fs.fresher.SystemE_commerce.enums.OrderStatus;
import fs.fresher.SystemE_commerce.enums.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SepayPaymentServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SepayPaymentService sepayPaymentService;

    private Order mockOrder;

    @BeforeEach
    void setUp() {
        // Set up configuration values
        ReflectionTestUtils.setField(sepayPaymentService, "sepayApiUrl", "https://api.sepay.vn");
        ReflectionTestUtils.setField(sepayPaymentService, "merchantId", "TEST_MERCHANT");
        ReflectionTestUtils.setField(sepayPaymentService, "baseUrl", "http://localhost:8080");

        setupTestData();
    }

    private void setupTestData() {
        mockOrder = new Order();
        mockOrder.setId(1L);
        mockOrder.setOrderNumber("ORD20240101120000001");
        mockOrder.setTrackingToken("tracking-token-123");
        mockOrder.setCustomerName("Nguyen Van A");
        mockOrder.setCustomerPhone("0123456789");
        mockOrder.setCustomerEmail("test@example.com");
        mockOrder.setShippingAddress("123 Test Street");
        mockOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
        mockOrder.setPaymentMethod(PaymentMethod.SEPAY);
        mockOrder.setTotalAmount(new BigDecimal("250.00"));
        mockOrder.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createPaymentUrl_WithValidOrder_ShouldReturnPaymentUrl() {
        // When
        String result = sepayPaymentService.createPaymentUrl(mockOrder);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("sepay-mock-payment"));
        assertTrue(result.contains("order=" + mockOrder.getOrderNumber()));
        assertTrue(result.contains("amount=" + mockOrder.getTotalAmount()));
        assertTrue(result.contains("http://localhost:8080"));
    }

    @Test
    void createPaymentUrl_ShouldGenerateCorrectMockUrl() {
        // When
        String result = sepayPaymentService.createPaymentUrl(mockOrder);

        // Then
        String expectedUrl = "http://localhost:8080/sepay-mock-payment?order=ORD20240101120000001&amount=250.00";
        assertEquals(expectedUrl, result);
    }

    @Test
    void createPaymentUrl_WithDifferentAmounts_ShouldGenerateCorrectUrls() {
        // Given
        mockOrder.setTotalAmount(new BigDecimal("100.50"));
        mockOrder.setOrderNumber("ORD20240101120000002");

        // When
        String result = sepayPaymentService.createPaymentUrl(mockOrder);

        // Then
        assertTrue(result.contains("order=ORD20240101120000002"));
        assertTrue(result.contains("amount=100.50"));
    }

    @Test
    void verifyPayment_WithValidData_ShouldReturnTrue() {
        // Given
        String transactionId = "TXN123456789";
        String orderNumber = "ORD20240101120000001";
        BigDecimal amount = new BigDecimal("250.00");

        // When
        boolean result = sepayPaymentService.verifyPayment(transactionId, orderNumber, amount);

        // Then
        assertTrue(result); // Mock implementation always returns true
    }

    @Test
    void verifyPayment_WithDifferentParameters_ShouldReturnTrue() {
        // Given
        String transactionId = "TXN987654321";
        String orderNumber = "ORD20240101120000002";
        BigDecimal amount = new BigDecimal("100.00");

        // When
        boolean result = sepayPaymentService.verifyPayment(transactionId, orderNumber, amount);

        // Then
        assertTrue(result); // Mock implementation always returns true
    }

    @Test
    void verifyPayment_WithNullTransactionId_ShouldReturnTrue() {
        // Given
        String transactionId = null;
        String orderNumber = "ORD20240101120000001";
        BigDecimal amount = new BigDecimal("250.00");

        // When
        boolean result = sepayPaymentService.verifyPayment(transactionId, orderNumber, amount);

        // Then
        assertTrue(result); // Mock implementation always returns true
    }

    @Test
    void createPaymentUrl_WithNullOrder_ShouldThrowException() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            sepayPaymentService.createPaymentUrl(null);
        });
    }

    @Test
    void createPaymentUrl_WithOrderHavingNullOrderNumber_ShouldGenerateUrl() {
        // Given
        mockOrder.setOrderNumber(null);

        // When
        String result = sepayPaymentService.createPaymentUrl(mockOrder);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("order=null"));
    }

    @Test
    void createPaymentUrl_WithOrderHavingNullAmount_ShouldGenerateUrl() {
        // Given
        mockOrder.setTotalAmount(null);

        // When
        String result = sepayPaymentService.createPaymentUrl(mockOrder);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("amount=null"));
    }

    @Test
    void createPaymentUrl_WithZeroAmount_ShouldGenerateUrl() {
        // Given
        mockOrder.setTotalAmount(BigDecimal.ZERO);

        // When
        String result = sepayPaymentService.createPaymentUrl(mockOrder);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("amount=0"));
    }

    @Test
    void createPaymentUrl_WithLargeAmount_ShouldGenerateUrl() {
        // Given
        mockOrder.setTotalAmount(new BigDecimal("999999.99"));

        // When
        String result = sepayPaymentService.createPaymentUrl(mockOrder);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("amount=999999.99"));
    }

    @Test
    void createPaymentUrl_WithSpecialCharactersInOrderNumber_ShouldGenerateUrl() {
        // Given
        mockOrder.setOrderNumber("ORD-2024/01/01-001");

        // When
        String result = sepayPaymentService.createPaymentUrl(mockOrder);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("order=ORD-2024/01/01-001"));
    }

    @Test
    void verifyPayment_WithEmptyStrings_ShouldReturnTrue() {
        // Given
        String transactionId = "";
        String orderNumber = "";
        BigDecimal amount = new BigDecimal("0");

        // When
        boolean result = sepayPaymentService.verifyPayment(transactionId, orderNumber, amount);

        // Then
        assertTrue(result); // Mock implementation always returns true
    }

    @Test
    void verifyPayment_WithNegativeAmount_ShouldReturnTrue() {
        // Given
        String transactionId = "TXN123";
        String orderNumber = "ORD123";
        BigDecimal amount = new BigDecimal("-100.00");

        // When
        boolean result = sepayPaymentService.verifyPayment(transactionId, orderNumber, amount);

        // Then
        assertTrue(result); // Mock implementation always returns true
    }
}