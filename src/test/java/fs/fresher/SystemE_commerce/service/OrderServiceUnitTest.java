package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.request.PlaceOrderRequest;
import fs.fresher.SystemE_commerce.dto.response.OrderResponse;
import fs.fresher.SystemE_commerce.entity.*;
import fs.fresher.SystemE_commerce.enums.OrderStatus;
import fs.fresher.SystemE_commerce.enums.PaymentMethod;
import fs.fresher.SystemE_commerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

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

    private PlaceOrderRequest validRequest;
    private Cart testCart;
    private ProductVariant testVariant;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Setup test data
        validRequest = new PlaceOrderRequest();
        validRequest.setCartId("test-cart-id");
        validRequest.setCustomerName("John Doe");
        validRequest.setCustomerPhone("0123456789");
        validRequest.setCustomerEmail("john.doe@example.com");
        validRequest.setShippingAddress("123 Test Street, Test City");
        validRequest.setNote("Test note");
        validRequest.setPaymentMethod(PaymentMethod.COD);

        // Setup test product
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");

        // Setup test variant
        testVariant = new ProductVariant();
        testVariant.setId(1L);
        testVariant.setProduct(testProduct);
        testVariant.setSku("TEST-SKU");
        testVariant.setSize("M");
        testVariant.setColor("Blue");
        testVariant.setPrice(new BigDecimal("99.99"));
        testVariant.setStockQuantity(10);

        // Setup test cart item
        CartItem cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setProductVariant(testVariant);
        cartItem.setQuantity(2);

        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(cartItem);

        // Setup test cart
        testCart = new Cart();
        testCart.setId(1L);
        testCart.setCartId("test-cart-id");
        testCart.setItems(cartItems);
    }

    @Test
    void testPlaceOrder_Success_DirectCart() {
        // Arrange
        when(cartRepository.findByCartIdWithItems("test-cart-id"))
                .thenReturn(Optional.of(testCart));
        when(inventoryReservationService.getAvailableStock(1L))
                .thenReturn(10);
        when(orderRepository.existsByOrderNumber(anyString()))
                .thenReturn(false);
        when(orderRepository.existsByTrackingToken(anyString()))
                .thenReturn(false);
        
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setOrderNumber("ORD20240120001");
        savedOrder.setTrackingToken("test-tracking-token");
        savedOrder.setCustomerName("John Doe");
        savedOrder.setCustomerPhone("0123456789");
        savedOrder.setCustomerEmail("john.doe@example.com");
        savedOrder.setShippingAddress("123 Test Street, Test City");
        savedOrder.setNote("Test note");
        savedOrder.setPaymentMethod(PaymentMethod.COD);
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        savedOrder.setTotalAmount(new BigDecimal("199.98"));
        savedOrder.setOrderItems(new ArrayList<>());
        
        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenReturn(new OrderItem());
        when(statusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenReturn(new OrderStatusHistory());
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenReturn(testVariant);

        // Act
        OrderResponse result = orderService.placeOrder(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals("ORD20240120001", result.getOrderNumber());
        assertEquals("test-tracking-token", result.getTrackingToken());
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        assertEquals(PaymentMethod.COD, result.getPaymentMethod());
        assertEquals(new BigDecimal("199.98"), result.getTotalAmount());
        assertEquals("John Doe", result.getCustomerName());

        // Verify interactions
        verify(cartRepository).findByCartIdWithItems("test-cart-id");
        verify(inventoryReservationService).getAvailableStock(1L);
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(statusHistoryRepository).save(any(OrderStatusHistory.class));
        verify(productVariantRepository).save(any(ProductVariant.class));
        verify(cartService).clearCart("test-cart-id");
    }

    @Test
    void testPlaceOrder_EmptyCart_ThrowsException() {
        // Arrange
        Cart emptyCart = new Cart();
        emptyCart.setCartId("empty-cart-id");
        emptyCart.setItems(new ArrayList<>());
        
        when(cartRepository.findByCartIdWithItems("empty-cart-id"))
                .thenReturn(Optional.of(emptyCart));

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setCartId("empty-cart-id");
        request.setCustomerName("John Doe");
        request.setCustomerPhone("0123456789");
        request.setCustomerEmail("john.doe@example.com");
        request.setShippingAddress("123 Test Street");
        request.setPaymentMethod(PaymentMethod.COD);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> orderService.placeOrder(request));
        assertEquals("Cart is empty", exception.getMessage());
    }

    @Test
    void testPlaceOrder_CartNotFound_ThrowsException() {
        // Arrange
        when(cartRepository.findByCartIdWithItems("non-existent-cart"))
                .thenReturn(Optional.empty());

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setCartId("non-existent-cart");
        request.setCustomerName("John Doe");
        request.setCustomerPhone("0123456789");
        request.setCustomerEmail("john.doe@example.com");
        request.setShippingAddress("123 Test Street");
        request.setPaymentMethod(PaymentMethod.COD);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> orderService.placeOrder(request));
        assertEquals("Cart not found", exception.getMessage());
    }

    @Test
    void testPlaceOrder_InsufficientStock_ThrowsException() {
        // Arrange
        when(cartRepository.findByCartIdWithItems("test-cart-id"))
                .thenReturn(Optional.of(testCart));
        when(inventoryReservationService.getAvailableStock(1L))
                .thenReturn(1); // Less than requested quantity of 2

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> orderService.placeOrder(validRequest));
        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }

    @Test
    void testPlaceOrder_MissingCustomerName_ThrowsException() {
        // Arrange
        validRequest.setCustomerName(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> orderService.placeOrder(validRequest));
        assertEquals("Customer name is required", exception.getMessage());
    }

    @Test
    void testPlaceOrder_MissingCustomerPhone_ThrowsException() {
        // Arrange
        validRequest.setCustomerPhone("");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> orderService.placeOrder(validRequest));
        assertEquals("Customer phone is required", exception.getMessage());
    }

    @Test
    void testPlaceOrder_MissingCustomerEmail_ThrowsException() {
        // Arrange
        validRequest.setCustomerEmail("   ");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> orderService.placeOrder(validRequest));
        assertEquals("Customer email is required", exception.getMessage());
    }

    @Test
    void testPlaceOrder_MissingShippingAddress_ThrowsException() {
        // Arrange
        validRequest.setShippingAddress(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> orderService.placeOrder(validRequest));
        assertEquals("Shipping address is required", exception.getMessage());
    }

    @Test
    void testPlaceOrder_MissingPaymentMethod_ThrowsException() {
        // Arrange
        validRequest.setPaymentMethod(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> orderService.placeOrder(validRequest));
        assertEquals("Payment method is required", exception.getMessage());
    }

    @Test
    void testPlaceOrder_Sepay_AwaitingPaymentStatus() {
        // Arrange
        validRequest.setPaymentMethod(PaymentMethod.SEPAY);
        
        when(cartRepository.findByCartIdWithItems("test-cart-id"))
                .thenReturn(Optional.of(testCart));
        when(inventoryReservationService.getAvailableStock(1L))
                .thenReturn(10);
        when(orderRepository.existsByOrderNumber(anyString()))
                .thenReturn(false);
        when(orderRepository.existsByTrackingToken(anyString()))
                .thenReturn(false);
        
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setOrderNumber("ORD20240120001");
        savedOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
        savedOrder.setPaymentMethod(PaymentMethod.SEPAY);
        savedOrder.setTotalAmount(new BigDecimal("199.98"));
        savedOrder.setOrderItems(new ArrayList<>());
        
        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenReturn(new OrderItem());
        when(statusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenReturn(new OrderStatusHistory());
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenReturn(testVariant);

        // Act
        OrderResponse result = orderService.placeOrder(validRequest);

        // Assert
        assertEquals(OrderStatus.AWAITING_PAYMENT, result.getStatus());
        assertEquals(PaymentMethod.SEPAY, result.getPaymentMethod());
    }

    @Test
    void testTrackOrder_Success() {
        // Arrange
        String trackingToken = "test-tracking-token";
        
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD20240120001");
        order.setTrackingToken(trackingToken);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setTotalAmount(new BigDecimal("199.98"));
        order.setCustomerName("John Doe");
        order.setShippingAddress("123 Test Street");
        order.setOrderItems(new ArrayList<>());
        order.setStatusHistory(new ArrayList<>());
        
        when(orderRepository.findByTrackingToken(trackingToken))
                .thenReturn(Optional.of(order));

        // Act
        var result = orderService.trackOrder(trackingToken);

        // Assert
        assertNotNull(result);
        assertEquals("ORD20240120001", result.getOrderNumber());
        assertEquals(OrderStatus.CONFIRMED, result.getCurrentStatus());
        assertEquals(PaymentMethod.COD, result.getPaymentMethod());
        assertEquals(new BigDecimal("199.98"), result.getTotalAmount());
        assertEquals("John Doe", result.getCustomerName());
    }

    @Test
    void testTrackOrder_NotFound_ThrowsException() {
        // Arrange
        String trackingToken = "non-existent-token";
        when(orderRepository.findByTrackingToken(trackingToken))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> orderService.trackOrder(trackingToken));
        assertEquals("Order not found", exception.getMessage());
    }
}