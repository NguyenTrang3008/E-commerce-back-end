package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.request.UpdateOrderStatusRequest;
import fs.fresher.SystemE_commerce.dto.response.AdminOrderDetailResponse;
import fs.fresher.SystemE_commerce.dto.response.AdminOrderListResponse;
import fs.fresher.SystemE_commerce.dto.response.PagedResponse;
import fs.fresher.SystemE_commerce.entity.*;
import fs.fresher.SystemE_commerce.enums.OrderStatus;
import fs.fresher.SystemE_commerce.enums.PaymentMethod;
import fs.fresher.SystemE_commerce.exception.BusinessException;
import fs.fresher.SystemE_commerce.exception.ErrorCode;
import fs.fresher.SystemE_commerce.exception.ResourceNotFoundException;
import fs.fresher.SystemE_commerce.exception.ValidationException;
import fs.fresher.SystemE_commerce.repository.OrderRepository;
import fs.fresher.SystemE_commerce.repository.OrderStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository statusHistoryRepository;

    @Mock
    private OrderStateMachineService stateMachineService;

    @Mock
    private AdminAuthService adminAuthService;

    @InjectMocks
    private AdminOrderService adminOrderService;

    private AdminUser mockAdmin;
    private Order mockOrder;
    private OrderItem mockOrderItem;
    private OrderStatusHistory mockStatusHistory;
    private UpdateOrderStatusRequest updateRequest;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        // Setup Admin User
        mockAdmin = new AdminUser();
        mockAdmin.setId(1L);
        mockAdmin.setUsername("admin");
        mockAdmin.setApiKey("valid-api-key");
        mockAdmin.setIsActive(true);

        // Setup Order Item
        mockOrderItem = new OrderItem();
        mockOrderItem.setId(1L);
        mockOrderItem.setProductName("Test Product");
        mockOrderItem.setSku("TEST-SKU");
        mockOrderItem.setSize("M");
        mockOrderItem.setColor("Red");
        mockOrderItem.setQuantity(2);
        mockOrderItem.setUnitPrice(new BigDecimal("100.00"));
        mockOrderItem.setTotalPrice(new BigDecimal("200.00"));

        // Setup Order
        mockOrder = new Order();
        mockOrder.setId(1L);
        mockOrder.setOrderNumber("ORD20240121140000001");
        mockOrder.setTrackingToken("tracking-token-123");
        mockOrder.setStatus(OrderStatus.CONFIRMED);
        mockOrder.setPaymentMethod(PaymentMethod.COD);
        mockOrder.setTotalAmount(new BigDecimal("200.00"));
        mockOrder.setCustomerName("John Doe");
        mockOrder.setCustomerPhone("0123456789");
        mockOrder.setCustomerEmail("john@example.com");
        mockOrder.setShippingAddress("123 Test Street");
        mockOrder.setNote("Test order");
        mockOrder.setCreatedAt(LocalDateTime.now());
        mockOrder.setUpdatedAt(LocalDateTime.now());
        mockOrder.setOrderItems(Arrays.asList(mockOrderItem));
        mockOrder.setStatusHistory(new ArrayList<>());

        // Setup Status History
        mockStatusHistory = new OrderStatusHistory();
        mockStatusHistory.setId(1L);
        mockStatusHistory.setOrder(mockOrder);
        mockStatusHistory.setPreviousStatus(OrderStatus.CONFIRMED);
        mockStatusHistory.setStatus(OrderStatus.PROCESSING);
        mockStatusHistory.setNote("Status updated by admin");
        mockStatusHistory.setChangedBy("admin");
        mockStatusHistory.setCreatedAt(LocalDateTime.now());

        // Setup Update Request
        updateRequest = new UpdateOrderStatusRequest();
        updateRequest.setStatus(OrderStatus.PROCESSING);
        updateRequest.setNote("Status updated by admin");
    }

    @Test
    void getOrders_WithValidApiKey_ShouldReturnPagedOrders() {
        // Given
        when(adminAuthService.authenticateByApiKey("valid-api-key"))
                .thenReturn(Optional.of(mockAdmin));

        List<Order> orders = Arrays.asList(mockOrder);
        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 1);
        when(orderRepository.findOrdersWithStatus(eq(OrderStatus.CONFIRMED), any(Pageable.class)))
                .thenReturn(orderPage);

        // When
        PagedResponse<AdminOrderListResponse> result = adminOrderService.getOrders(
                "valid-api-key", OrderStatus.CONFIRMED, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());

        AdminOrderListResponse orderResponse = result.getContent().get(0);
        assertEquals(mockOrder.getId(), orderResponse.getOrderId());
        assertEquals(mockOrder.getOrderNumber(), orderResponse.getOrderNumber());
        assertEquals(mockOrder.getStatus(), orderResponse.getStatus());
        assertEquals(2, orderResponse.getTotalItems()); // quantity from mockOrderItem

        verify(adminAuthService).authenticateByApiKey("valid-api-key");
        verify(orderRepository).findOrdersWithStatus(eq(OrderStatus.CONFIRMED), any(Pageable.class));
    }

    @Test
    void getOrders_WithInvalidApiKey_ShouldThrowBusinessException() {
        // Given
        when(adminAuthService.authenticateByApiKey("invalid-api-key"))
                .thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminOrderService.getOrders("invalid-api-key", OrderStatus.CONFIRMED, 0, 10);
        });

        assertEquals("Invalid API key", exception.getMessage());
        verify(adminAuthService).authenticateByApiKey("invalid-api-key");
        verify(orderRepository, never()).findOrdersWithStatus(any(), any());
    }

    @Test
    void getOrderDetail_WithValidApiKeyAndOrderId_ShouldReturnOrderDetail() {
        // Given
        when(adminAuthService.authenticateByApiKey("valid-api-key"))
                .thenReturn(Optional.of(mockAdmin));
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(mockOrder));
        when(stateMachineService.getValidTransitions(OrderStatus.CONFIRMED))
                .thenReturn(Arrays.asList(OrderStatus.PROCESSING, OrderStatus.CANCELLED));

        // When
        AdminOrderDetailResponse result = adminOrderService.getOrderDetail("valid-api-key", 1L);

        // Then
        assertNotNull(result);
        assertEquals(mockOrder.getId(), result.getOrderId());
        assertEquals(mockOrder.getOrderNumber(), result.getOrderNumber());
        assertEquals(mockOrder.getStatus(), result.getStatus());
        assertEquals(1, result.getItems().size());
        assertEquals(2, result.getAvailableTransitions().size());

        verify(adminAuthService).authenticateByApiKey("valid-api-key");
        verify(orderRepository).findById(1L);
        verify(stateMachineService).getValidTransitions(OrderStatus.CONFIRMED);
    }

    @Test
    void getOrderDetail_WithInvalidApiKey_ShouldThrowBusinessException() {
        // Given
        when(adminAuthService.authenticateByApiKey("invalid-api-key"))
                .thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminOrderService.getOrderDetail("invalid-api-key", 1L);
        });

        assertEquals("Invalid API key", exception.getMessage());
        verify(orderRepository, never()).findById(any());
    }

    @Test
    void getOrderDetail_WithNonExistentOrder_ShouldThrowResourceNotFoundException() {
        // Given
        when(adminAuthService.authenticateByApiKey("valid-api-key"))
                .thenReturn(Optional.of(mockAdmin));
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            adminOrderService.getOrderDetail("valid-api-key", 999L);
        });

        assertTrue(exception.getMessage().contains("Order not found with ID: 999"));
        verify(adminAuthService).authenticateByApiKey("valid-api-key");
        verify(orderRepository).findById(999L);
    }

    @Test
    void updateOrderStatus_WithValidData_ShouldUpdateSuccessfully() {
        // Given
        when(adminAuthService.authenticateByApiKey("valid-api-key"))
                .thenReturn(Optional.of(mockAdmin));
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(mockOrder));
        doNothing().when(stateMachineService)
                .validateTransition(OrderStatus.CONFIRMED, OrderStatus.PROCESSING, PaymentMethod.COD);
        when(orderRepository.save(any(Order.class)))
                .thenReturn(mockOrder);
        when(statusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenReturn(mockStatusHistory);
        when(stateMachineService.getValidTransitions(OrderStatus.PROCESSING))
                .thenReturn(Arrays.asList(OrderStatus.SHIPPED, OrderStatus.CANCELLED));

        // When
        AdminOrderDetailResponse result = adminOrderService.updateOrderStatus(
                "valid-api-key", 1L, updateRequest);

        // Then
        assertNotNull(result);
        assertEquals(mockOrder.getId(), result.getOrderId());
        
        verify(adminAuthService).authenticateByApiKey("valid-api-key");
        verify(orderRepository).findById(1L);
        verify(stateMachineService).validateTransition(OrderStatus.CONFIRMED, OrderStatus.PROCESSING, PaymentMethod.COD);
        verify(orderRepository).save(mockOrder);
        verify(statusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void updateOrderStatus_WithNullStatus_ShouldThrowValidationException() {
        // Given
        when(adminAuthService.authenticateByApiKey("valid-api-key"))
                .thenReturn(Optional.of(mockAdmin));
        
        UpdateOrderStatusRequest invalidRequest = new UpdateOrderStatusRequest();
        invalidRequest.setStatus(null);
        invalidRequest.setNote("Test note");

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            adminOrderService.updateOrderStatus("valid-api-key", 1L, invalidRequest);
        });

        assertEquals("Status is required", exception.getMessage());
        verify(adminAuthService).authenticateByApiKey("valid-api-key");
        verify(orderRepository, never()).findById(any());
    }

    @Test
    void updateOrderStatus_SystemCall_ShouldUpdateSuccessfully() {
        // Given
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(mockOrder));
        doNothing().when(stateMachineService)
                .validateTransition(OrderStatus.CONFIRMED, OrderStatus.PROCESSING, PaymentMethod.COD);
        when(orderRepository.save(any(Order.class)))
                .thenReturn(mockOrder);
        when(statusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenReturn(mockStatusHistory);
        when(stateMachineService.getValidTransitions(OrderStatus.PROCESSING))
                .thenReturn(Arrays.asList(OrderStatus.SHIPPED, OrderStatus.CANCELLED));

        // When
        AdminOrderDetailResponse result = adminOrderService.updateOrderStatus(1L, updateRequest, "SYSTEM");

        // Then
        assertNotNull(result);
        assertEquals(mockOrder.getId(), result.getOrderId());
        
        verify(orderRepository).findById(1L);
        verify(stateMachineService).validateTransition(OrderStatus.CONFIRMED, OrderStatus.PROCESSING, PaymentMethod.COD);
        verify(orderRepository).save(mockOrder);
        verify(statusHistoryRepository).save(any(OrderStatusHistory.class));
        // Should not call adminAuthService for system calls
        verify(adminAuthService, never()).authenticateByApiKey(any());
    }

    @Test
    void updateOrderStatus_SystemCall_WithNullStatus_ShouldThrowValidationException() {
        // Given
        UpdateOrderStatusRequest invalidRequest = new UpdateOrderStatusRequest();
        invalidRequest.setStatus(null);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            adminOrderService.updateOrderStatus(1L, invalidRequest, "SYSTEM");
        });

        assertEquals("Status is required", exception.getMessage());
        verify(orderRepository, never()).findById(any());
    }

    @Test
    void updateOrderStatus_SystemCall_WithNonExistentOrder_ShouldThrowResourceNotFoundException() {
        // Given
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            adminOrderService.updateOrderStatus(999L, updateRequest, "SYSTEM");
        });

        assertTrue(exception.getMessage().contains("Order not found with ID: 999"));
        verify(orderRepository).findById(999L);
    }

    @Test
    void getOrders_WithNullStatus_ShouldReturnAllOrders() {
        // Given
        when(adminAuthService.authenticateByApiKey("valid-api-key"))
                .thenReturn(Optional.of(mockAdmin));

        List<Order> orders = Arrays.asList(mockOrder);
        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 1);
        when(orderRepository.findOrdersWithStatus(eq(null), any(Pageable.class)))
                .thenReturn(orderPage);

        // When
        PagedResponse<AdminOrderListResponse> result = adminOrderService.getOrders(
                "valid-api-key", null, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(orderRepository).findOrdersWithStatus(eq(null), any(Pageable.class));
    }

    @Test
    void getOrders_WithEmptyResult_ShouldReturnEmptyPage() {
        // Given
        when(adminAuthService.authenticateByApiKey("valid-api-key"))
                .thenReturn(Optional.of(mockAdmin));

        Page<Order> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0);
        when(orderRepository.findOrdersWithStatus(eq(OrderStatus.CONFIRMED), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        PagedResponse<AdminOrderListResponse> result = adminOrderService.getOrders(
                "valid-api-key", OrderStatus.CONFIRMED, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
    }

    @Test
    void updateOrderStatus_WithInvalidTransition_ShouldThrowException() {
        // Given
        when(adminAuthService.authenticateByApiKey("valid-api-key"))
                .thenReturn(Optional.of(mockAdmin));
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(mockOrder));
        doThrow(new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Invalid status transition"))
                .when(stateMachineService)
                .validateTransition(OrderStatus.CONFIRMED, OrderStatus.PROCESSING, PaymentMethod.COD);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            adminOrderService.updateOrderStatus("valid-api-key", 1L, updateRequest);
        });

        assertEquals("Invalid status transition", exception.getMessage());
        verify(orderRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
    }
}