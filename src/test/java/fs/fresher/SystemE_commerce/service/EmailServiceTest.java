package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.entity.Order;
import fs.fresher.SystemE_commerce.entity.OrderItem;
import fs.fresher.SystemE_commerce.enums.OrderStatus;
import fs.fresher.SystemE_commerce.enums.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private Order mockOrder;
    private OrderItem mockOrderItem1;
    private OrderItem mockOrderItem2;

    @BeforeEach
    void setUp() {
        // Set up configuration
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@ecommerce.com");
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        
        setupTestData();
    }

    private void setupTestData() {
        // Setup Order Items
        mockOrderItem1 = new OrderItem();
        mockOrderItem1.setId(1L);
        mockOrderItem1.setProductName("Test Product 1");
        mockOrderItem1.setSku("TEST-SKU-001");
        mockOrderItem1.setSize("M");
        mockOrderItem1.setColor("Red");
        mockOrderItem1.setQuantity(2);
        mockOrderItem1.setUnitPrice(new BigDecimal("100.00"));
        mockOrderItem1.setTotalPrice(new BigDecimal("200.00"));

        mockOrderItem2 = new OrderItem();
        mockOrderItem2.setId(2L);
        mockOrderItem2.setProductName("Test Product 2");
        mockOrderItem2.setSku("TEST-SKU-002");
        mockOrderItem2.setSize("L");
        mockOrderItem2.setColor("Blue");
        mockOrderItem2.setQuantity(1);
        mockOrderItem2.setUnitPrice(new BigDecimal("150.00"));
        mockOrderItem2.setTotalPrice(new BigDecimal("150.00"));

        // Setup Order
        mockOrder = new Order();
        mockOrder.setId(1L);
        mockOrder.setOrderNumber("ORD20240121140000001");
        mockOrder.setTrackingToken("tracking-token-123");
        mockOrder.setStatus(OrderStatus.CONFIRMED);
        mockOrder.setPaymentMethod(PaymentMethod.COD);
        mockOrder.setTotalAmount(new BigDecimal("350.00"));
        mockOrder.setCustomerName("Nguyen Van Test");
        mockOrder.setCustomerPhone("0987654321");
        mockOrder.setCustomerEmail("test@example.com");
        mockOrder.setShippingAddress("123 Test Street, District 1, Ho Chi Minh City");
        mockOrder.setNote("Test order");
        mockOrder.setCreatedAt(LocalDateTime.of(2024, 1, 21, 14, 0, 0));
        mockOrder.setOrderItems(Arrays.asList(mockOrderItem1, mockOrderItem2));
    }

    @Test
    void sendOrderConfirmation_WithEmailEnabled_ShouldSendEmail() {
        // Given
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendOrderConfirmation(mockOrder);

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("noreply@ecommerce.com", sentMessage.getFrom());
        assertEquals("test@example.com", sentMessage.getTo()[0]);
        assertEquals("Xác nhận đơn hàng - ORD20240121140000001", sentMessage.getSubject());
        
        String emailBody = sentMessage.getText();
        assertNotNull(emailBody);
        assertTrue(emailBody.contains("Nguyen Van Test"));
        assertTrue(emailBody.contains("ORD20240121140000001"));
        assertTrue(emailBody.contains("350 VNĐ"));
        assertTrue(emailBody.contains("Test Product 1"));
        assertTrue(emailBody.contains("Test Product 2"));
        assertTrue(emailBody.contains("http://localhost:8080/track/tracking-token-123"));
        assertTrue(emailBody.contains("Thanh toán khi nhận hàng"));
    }

    @Test
    void sendOrderConfirmation_WithEmailDisabled_ShouldLogEmailContent() {
        // Given
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);

        // When
        emailService.sendOrderConfirmation(mockOrder);

        // Then
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        // Email content should be logged (we can't easily test log output in unit tests)
    }

    @Test
    void sendOrderConfirmation_WithSepayPayment_ShouldIncludeSepayText() {
        // Given
        mockOrder.setPaymentMethod(PaymentMethod.SEPAY);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendOrderConfirmation(mockOrder);

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String emailBody = sentMessage.getText();
        assertTrue(emailBody.contains("Thanh toán online qua SePay"));
        assertTrue(emailBody.contains("Vui lòng hoàn tất thanh toán"));
    }

    @Test
    void sendOrderConfirmation_WithCodPayment_ShouldIncludeCodText() {
        // Given
        mockOrder.setPaymentMethod(PaymentMethod.COD);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendOrderConfirmation(mockOrder);

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String emailBody = sentMessage.getText();
        assertTrue(emailBody.contains("Thanh toán khi nhận hàng"));
        assertTrue(emailBody.contains("giao đến địa chỉ của bạn"));
    }

    @Test
    void sendOrderConfirmation_WithMailSenderException_ShouldNotThrowException() {
        // Given
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            emailService.sendOrderConfirmation(mockOrder);
        });

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOrderConfirmation_WithNullOrderItems_ShouldHandleGracefully() {
        // Given
        mockOrder.setOrderItems(null);

        // When & Then - Should not throw exception (handled by try-catch in EmailService)
        assertDoesNotThrow(() -> {
            emailService.sendOrderConfirmation(mockOrder);
        });
        
        // Verify that mailSender.send() was never called due to exception in buildOrderConfirmationEmail
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOrderConfirmation_WithEmptyOrderItems_ShouldHandleGracefully() {
        // Given
        mockOrder.setOrderItems(Arrays.asList());
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendOrderConfirmation(mockOrder);

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String emailBody = sentMessage.getText();
        assertNotNull(emailBody);
        assertTrue(emailBody.contains("Sản phẩm đã đặt:"));
    }

    @Test
    void sendOrderConfirmation_WithItemsWithoutSizeAndColor_ShouldFormatCorrectly() {
        // Given
        mockOrderItem1.setSize(null);
        mockOrderItem1.setColor(null);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendOrderConfirmation(mockOrder);

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String emailBody = sentMessage.getText();
        assertTrue(emailBody.contains("Test Product 1"));
        assertFalse(emailBody.contains("(Size: null)"));
        assertFalse(emailBody.contains("(Màu: null)"));
    }

    @Test
    void sendOrderConfirmation_WithDifferentBaseUrl_ShouldUseCorrectTrackingUrl() {
        // Given
        ReflectionTestUtils.setField(emailService, "baseUrl", "https://production.example.com");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendOrderConfirmation(mockOrder);

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String emailBody = sentMessage.getText();
        assertTrue(emailBody.contains("https://production.example.com/track/tracking-token-123"));
    }

    @Test
    void sendOrderConfirmation_WithDifferentFromEmail_ShouldUseCorrectFromAddress() {
        // Given
        ReflectionTestUtils.setField(emailService, "fromEmail", "orders@mystore.com");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendOrderConfirmation(mockOrder);

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("orders@mystore.com", sentMessage.getFrom());
    }

    @Test
    void sendOrderConfirmation_WithLargeAmount_ShouldFormatCorrectly() {
        // Given
        mockOrder.setTotalAmount(new BigDecimal("1234567.89"));
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendOrderConfirmation(mockOrder);

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String emailBody = sentMessage.getText();
        assertTrue(emailBody.contains("1,234,568 VNĐ")); // Formatted with commas, rounded
    }

    @Test
    void sendOrderConfirmation_WithSpecialCharactersInCustomerName_ShouldHandleCorrectly() {
        // Given
        mockOrder.setCustomerName("Nguyễn Văn Đức");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendOrderConfirmation(mockOrder);

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String emailBody = sentMessage.getText();
        assertTrue(emailBody.contains("Nguyễn Văn Đức"));
    }

    @Test
    void sendOrderConfirmation_WithLongShippingAddress_ShouldIncludeFullAddress() {
        // Given
        String longAddress = "Số 123, Đường Nguyễn Trãi, Phường Bến Thành, Quận 1, Thành phố Hồ Chí Minh, Việt Nam";
        mockOrder.setShippingAddress(longAddress);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendOrderConfirmation(mockOrder);

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String emailBody = sentMessage.getText();
        assertTrue(emailBody.contains(longAddress));
    }

    @Test
    void sendOrderConfirmation_WithNullNote_ShouldHandleGracefully() {
        // Given
        mockOrder.setNote(null);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            emailService.sendOrderConfirmation(mockOrder);
        });

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}