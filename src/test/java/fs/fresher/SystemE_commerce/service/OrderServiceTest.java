package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.request.PlaceOrderRequest;
import fs.fresher.SystemE_commerce.dto.response.OrderResponse;
import fs.fresher.SystemE_commerce.dto.response.OrderTrackingResponse;
import fs.fresher.SystemE_commerce.entity.*;
import fs.fresher.SystemE_commerce.enums.OrderStatus;
import fs.fresher.SystemE_commerce.enums.PaymentMethod;
import fs.fresher.SystemE_commerce.exception.ResourceNotFoundException;
import fs.fresher.SystemE_commerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private OrderItemRepository orderItemRepository;
    
    @Mock
    private OrderStatusHistoryRepository statusHistoryRepository;
    
    @Mock
    private CheckoutService checkoutService;
    
    @Mock
    private CartService cartService;
    
    @Mock
    private CartRepository cartRepository;
    
    @Mock
    private StockReservationRepository stockReservationRepository;
    
    @Mock
    private ProductVariantRepository productVariantRepository;
    
    @Mock
    private InventoryReservationService inventoryReservationService;
    
    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderService orderService;

    private PlaceOrderRequest codOrderRequest;
    private PlaceOrderRequest sepayOrderRequest;
    private Cart mockCart;
    private CheckoutSession mockCheckoutSession;
    private Order mockOrder;
    private ProductVariant mockVariant;
    private Product mockProduct;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "baseUrl", "http://localhost:8080");
        
        // Setup test data
        setupTestData();
    }

    private void setupTestData() {
        // COD Order Request
        codOrderRequest = new PlaceOrderRequest();
        codOrderRequest.setCustomerName("Nguyen Van A");
        codOrderRequest.setCustomerPhone("0123456789");
        codOrderRequest.setCustomerEmail("test@example.com");
        codOrderRequest.setShippingAddress("123 Test Street");
        codOrderRequest.setNote("Test order");
        codOrderRequest.setPaymentMethod(PaymentMethod.COD);
        codOrderRequest.setCartId("cart-123");

        // SePay Order Request
        sepayOrderRequest = new PlaceOrderRequest();
        sepayOrderRequest.setCustomerName("Nguyen Van B");
        sepayOrderRequest.setCustomerPhone("0987654321");
        sepayOrderRequest.setCustomerEmail("sepay@example.com");
        sepayOrderRequest.setShippingAddress("456 SePay Street");
        sepayOrderRequest.setPaymentMethod(PaymentMethod.SEPAY);
        sepayOrderRequest.setCheckoutToken("checkout-token-123");

        // Mock Product and Variant
        mockProduct = new Product();
        mockProduct.setId(1L);
        mockProduct.setName("Test Product");

        mockVariant = new ProductVariant();
        mockVariant.setId(1L);
        mockVariant.setProduct(mockProduct);
        mockVariant.setSku("TEST-SKU-001");
        mockVariant.setSize("M");
        mockVariant.setColor("Red");
        mockVariant.setPrice(new BigDecimal("100.00"));
        mockVariant.setStockQuantity(10); // Add stock quantity

        // Mock Cart Item
        CartItem mockCartItem = new CartItem();
        mockCartItem.setId(1L);
        mockCartItem.setProductVariant(mockVariant);
        mockCartItem.setQuantity(2);

        // Mock Cart
        mockCart = new Cart();
        mockCart.setCartId("cart-123");
        mockCart.setItems(Arrays.asList(mockCartItem));

        // Mock Checkout Session
        mockCheckoutSession = new CheckoutSession();
        mockCheckoutSession.setCheckoutToken("checkout-token-123");
        mockCheckoutSession.setCartId("cart-123");
        mockCheckoutSession.setIsUsed(false);
        mockCheckoutSession.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        // Mock Order
        mockOrder = new Order();
        mockOrder.setId(1L);
        mockOrder.setOrderNumber("ORD20240101120000001");
        mockOrder.setTrackingToken("tracking-token-123");
        mockOrder.setCustomerName("Test Customer");
        mockOrder.setStatus(OrderStatus.CONFIRMED);
        mockOrder.setPaymentMethod(PaymentMethod.COD);
        mockOrder.setTotalAmount(new BigDecimal("200.00"));
        mockOrder.setCreatedAt(LocalDateTime.now());
        
        // Mock OrderItem
        OrderItem mockOrderItem = new OrderItem();
        mockOrderItem.setId(1L);
        mockOrderItem.setOrder(mockOrder);
        mockOrderItem.setProductVariant(mockVariant);
        mockOrderItem.setProductName("Test Product");
        mockOrderItem.setSku("TEST-SKU-001");
        mockOrderItem.setSize("M");
        mockOrderItem.setColor("Red");
        mockOrderItem.setQuantity(2);
        mockOrderItem.setUnitPrice(new BigDecimal("100.00"));
        mockOrderItem.setTotalPrice(new BigDecimal("200.00"));
        
        mockOrder.setOrderItems(Arrays.asList(mockOrderItem));
    }

    @Test
    void placeOrder_WithCOD_ShouldCreateConfirmedOrder() {
        // Given
        when(cartRepository.findByCartIdWithItems("cart-123")).thenReturn(Optional.of(mockCart));
        when(inventoryReservationService.getAvailableStock(1L)).thenReturn(10); // Sufficient stock
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(orderRepository.existsByTrackingToken(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(statusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());

        // When
        OrderResponse result = orderService.placeOrder(codOrderRequest);

        // Then
        assertNotNull(result);
        assertEquals("ORD20240101120000001", result.getOrderNumber());
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        assertEquals(PaymentMethod.COD, result.getPaymentMethod());
        
        verify(cartService).clearCart("cart-123");
        verify(emailService).sendOrderConfirmation(any(Order.class));
        verify(productVariantRepository).save(any(ProductVariant.class)); // Stock reduction
    }

    @Test
    void placeOrder_WithSePay_ShouldCreateAwaitingPaymentOrder() {
        // Given
        mockOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
        mockOrder.setPaymentMethod(PaymentMethod.SEPAY);
        
        when(checkoutService.validateCheckoutSession("checkout-token-123")).thenReturn(mockCheckoutSession);
        when(cartRepository.findByCartIdWithItems("cart-123")).thenReturn(Optional.of(mockCart));
        when(inventoryReservationService.getAvailableStock(1L)).thenReturn(10); // Sufficient stock
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(orderRepository.existsByTrackingToken(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(statusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());

        // When
        OrderResponse result = orderService.placeOrder(sepayOrderRequest);

        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.AWAITING_PAYMENT, result.getStatus());
        assertEquals(PaymentMethod.SEPAY, result.getPaymentMethod());
        
        verify(checkoutService).validateCheckoutSession("checkout-token-123");
        verify(checkoutService).confirmReservations("checkout-token-123");
        verify(checkoutService).markSessionAsUsed("checkout-token-123");
        verify(emailService).sendOrderConfirmation(any(Order.class));
    }

    @Test
    void placeOrder_WithEmptyCart_ShouldThrowException() {
        // Given
        Cart emptyCart = new Cart();
        emptyCart.setCartId("empty-cart");
        emptyCart.setItems(Arrays.asList());
        
        codOrderRequest.setCartId("empty-cart");
        when(cartRepository.findByCartIdWithItems("empty-cart")).thenReturn(Optional.of(emptyCart));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> orderService.placeOrder(codOrderRequest));
        assertEquals("Cart is empty", exception.getMessage());
    }

    @Test
    void placeOrder_WithInvalidCart_ShouldThrowException() {
        // Given
        when(cartRepository.findByCartIdWithItems("invalid-cart")).thenReturn(Optional.empty());
        codOrderRequest.setCartId("invalid-cart");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> orderService.placeOrder(codOrderRequest));
        assertEquals("Cart not found", exception.getMessage());
    }

    @Test
    void placeOrder_WithStockReservationFailure_ShouldThrowException() {
        // Given
        when(cartRepository.findByCartIdWithItems("cart-123")).thenReturn(Optional.of(mockCart));
        when(inventoryReservationService.getAvailableStock(1L)).thenReturn(1); // Insufficient stock (need 2)

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> orderService.placeOrder(codOrderRequest));
        assertTrue(exception.getMessage().contains("Insufficient stock for product"));
    }

    @Test
    void placeOrder_WithInvalidCheckoutToken_ShouldThrowException() {
        // Given
        when(checkoutService.validateCheckoutSession("invalid-token"))
            .thenThrow(new RuntimeException("Invalid checkout token"));
        sepayOrderRequest.setCheckoutToken("invalid-token");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> orderService.placeOrder(sepayOrderRequest));
        assertEquals("Invalid checkout token", exception.getMessage());
    }

    @Test
    void placeOrder_WithoutCartIdOrCheckoutToken_ShouldThrowException() {
        // Given
        PlaceOrderRequest invalidRequest = new PlaceOrderRequest();
        invalidRequest.setCustomerName("Test");
        invalidRequest.setPaymentMethod(PaymentMethod.COD);
        // No cartId or checkoutToken

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> orderService.placeOrder(invalidRequest));
        assertEquals("Either checkoutToken or cartId is required", exception.getMessage());
    }

    @Test
    void trackOrder_WithValidToken_ShouldReturnOrderTracking() {
        // Given
        OrderItem mockOrderItem = new OrderItem();
        mockOrderItem.setProductName("Test Product");
        mockOrderItem.setSku("TEST-SKU-001");
        mockOrderItem.setSize("M");
        mockOrderItem.setColor("Red");
        mockOrderItem.setQuantity(2);
        mockOrderItem.setUnitPrice(new BigDecimal("100.00"));
        
        OrderStatusHistory mockHistory = new OrderStatusHistory();
        mockHistory.setStatus(OrderStatus.CONFIRMED);
        mockHistory.setNote("Order placed");
        mockHistory.setCreatedAt(LocalDateTime.now());
        
        mockOrder.setOrderItems(Arrays.asList(mockOrderItem));
        mockOrder.setStatusHistory(Arrays.asList(mockHistory));
        
        when(orderRepository.findByTrackingToken("tracking-token-123"))
            .thenReturn(Optional.of(mockOrder));

        // When
        OrderTrackingResponse result = orderService.trackOrder("tracking-token-123");

        // Then
        assertNotNull(result);
        assertEquals("ORD20240101120000001", result.getOrderNumber());
        assertEquals(OrderStatus.CONFIRMED, result.getCurrentStatus());
        assertEquals(1, result.getItems().size());
        assertEquals(1, result.getStatusHistory().size());
    }

    @Test
    void trackOrder_WithInvalidToken_ShouldThrowException() {
        // Given
        when(orderRepository.findByTrackingToken("invalid-token"))
            .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
            () -> orderService.trackOrder("invalid-token"));
        assertEquals("Order not found with tracking token: invalid-token", exception.getMessage());
    }

    @Test
    void placeOrder_ShouldGenerateUniqueOrderNumber() {
        // Given
        when(cartRepository.findByCartIdWithItems("cart-123")).thenReturn(Optional.of(mockCart));
        when(inventoryReservationService.getAvailableStock(1L)).thenReturn(10); // Sufficient stock
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(orderRepository.existsByTrackingToken(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            // Set orderItems to avoid NullPointerException
            OrderItem mockOrderItem = new OrderItem();
            mockOrderItem.setId(1L);
            mockOrderItem.setOrder(order);
            mockOrderItem.setProductVariant(mockVariant);
            mockOrderItem.setProductName("Test Product");
            mockOrderItem.setSku("TEST-SKU-001");
            mockOrderItem.setSize("M");
            mockOrderItem.setColor("Red");
            mockOrderItem.setQuantity(2);
            mockOrderItem.setUnitPrice(new BigDecimal("100.00"));
            mockOrderItem.setTotalPrice(new BigDecimal("200.00"));
            order.setOrderItems(Arrays.asList(mockOrderItem));
            return order;
        });
        when(statusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());

        // When
        OrderResponse result = orderService.placeOrder(codOrderRequest);

        // Then
        assertNotNull(result.getOrderNumber());
        assertTrue(result.getOrderNumber().startsWith("ORD"));
        assertTrue(result.getOrderNumber().length() > 10);
    }

    @Test
    void placeOrder_ShouldGenerateTrackingUrl() {
        // Given
        when(cartRepository.findByCartIdWithItems("cart-123")).thenReturn(Optional.of(mockCart));
        when(inventoryReservationService.getAvailableStock(1L)).thenReturn(10); // Sufficient stock
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(orderRepository.existsByTrackingToken(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(statusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());

        // When
        OrderResponse result = orderService.placeOrder(codOrderRequest);

        // Then
        assertNotNull(result.getTrackingUrl());
        assertTrue(result.getTrackingUrl().contains("http://localhost:8080/track/"));
        assertTrue(result.getTrackingUrl().contains(result.getTrackingToken()));
    }
}