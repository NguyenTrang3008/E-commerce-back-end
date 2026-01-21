package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.request.PlaceOrderRequest;
import fs.fresher.SystemE_commerce.dto.response.OrderResponse;
import fs.fresher.SystemE_commerce.dto.response.OrderTrackingResponse;
import fs.fresher.SystemE_commerce.entity.*;
import fs.fresher.SystemE_commerce.enums.OrderStatus;
import fs.fresher.SystemE_commerce.enums.PaymentMethod;
import fs.fresher.SystemE_commerce.exception.ErrorCode;
import fs.fresher.SystemE_commerce.exception.ResourceNotFoundException;
import fs.fresher.SystemE_commerce.repository.*;
import fs.fresher.SystemE_commerce.validator.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = false)
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final CheckoutService checkoutService;
    private final CartService cartService;
    private final CartRepository cartRepository;
    private final StockReservationRepository stockReservationRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryReservationService inventoryReservationService;
    private final EmailService emailService;
    private final ValidationService validationService;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        // Validate request
        validationService.validatePlaceOrderRequest(request);
        if (request == null) {
            throw new IllegalArgumentException("Order request cannot be null");
        }
        
        List<CartItem> cartItems;
        
        // Get cart items either from checkout session or directly from cart
        if (request.getCheckoutToken() != null) {
            CheckoutSession session = checkoutService.validateCheckoutSession(request.getCheckoutToken());
            Cart cart = cartRepository.findByCartIdWithItems(session.getCartId())
                    .orElseThrow(() -> new RuntimeException("Cart not found"));
            cartItems = cart.getItems();
            
            // Validate cart items for checkout session
            if (cartItems == null || cartItems.isEmpty()) {
                throw new RuntimeException("Cart is empty");
            }
        } else if (request.getCartId() != null) {
            Cart cart = cartRepository.findByCartIdWithItems(request.getCartId())
                    .orElseThrow(() -> new RuntimeException("Cart not found"));
            cartItems = cart.getItems();
            
            if (cartItems == null || cartItems.isEmpty()) {
                throw new RuntimeException("Cart is empty");
            }
        } else {
            throw new RuntimeException("Either checkoutToken or cartId is required");
        }
        
        // Validate customer information
        if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        if (request.getCustomerPhone() == null || request.getCustomerPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer phone is required");
        }
        if (request.getCustomerEmail() == null || request.getCustomerEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer email is required");
        }
        if (request.getShippingAddress() == null || request.getShippingAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Shipping address is required");
        }
        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Payment method is required");
        }
        
        // Validate cart items and calculate total amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            // Ensure ProductVariant and Product are not null
            if (cartItem.getProductVariant() == null) {
                throw new RuntimeException("Product variant not found for cart item");
            }
            if (cartItem.getProductVariant().getProduct() == null) {
                throw new RuntimeException("Product not found for product variant");
            }
            if (cartItem.getProductVariant().getPrice() == null) {
                throw new RuntimeException("Product variant price not found");
            }
            
            // Always check stock before calculating total
            int availableStock = inventoryReservationService.getAvailableStock(cartItem.getProductVariant().getId());
            if (availableStock < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + cartItem.getProductVariant().getSku() + 
                    ". Available: " + availableStock + ", Required: " + cartItem.getQuantity());
            }
            
            BigDecimal itemTotal = cartItem.getProductVariant().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }
        
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Order total amount must be greater than zero");
        }
        
        // Create order with retry mechanism for unique constraints
        Order order = createOrderWithRetry(request, totalAmount);
        
        // Create order items - cascade will handle saving automatically
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            ProductVariant variant = cartItem.getProductVariant();
            
            // Ensure ProductVariant and Product are not null
            if (variant == null) {
                throw new RuntimeException("Product variant not found for cart item");
            }
            if (variant.getProduct() == null) {
                throw new RuntimeException("Product not found for product variant");
            }
            
            // Final stock check before creating order item
            int availableStock = inventoryReservationService.getAvailableStock(variant.getId());
            if (availableStock < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + variant.getSku() + 
                    ". Available: " + availableStock + ", Required: " + cartItem.getQuantity());
            }
            
            BigDecimal itemTotal = variant.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            
            // Create order item (snapshot of current data)
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductVariant(variant);
            orderItem.setProductName(variant.getProduct().getName());
            orderItem.setSku(variant.getSku());
            orderItem.setSize(variant.getSize());
            orderItem.setColor(variant.getColor());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(variant.getPrice());
            orderItem.setTotalPrice(itemTotal);
            
            // Add to order items list - cascade will handle saving
            orderItems.add(orderItem);
        }
        
        // Set the order items to the order - cascade will save them automatically
        order.setOrderItems(orderItems);
        
        // Set initial status based on payment method
        OrderStatus initialStatus = request.getPaymentMethod() == PaymentMethod.COD 
                ? OrderStatus.CONFIRMED 
                : OrderStatus.AWAITING_PAYMENT;
        
        // Create initial status history
        createStatusHistory(order, initialStatus, "Order placed");
        
        // Confirm reservations or reduce stock directly
        if (request.getCheckoutToken() != null) {
            checkoutService.confirmReservations(request.getCheckoutToken());
            checkoutService.markSessionAsUsed(request.getCheckoutToken());
        } else {
            // For direct cart orders, reduce stock after final validation
            for (CartItem cartItem : cartItems) {
                ProductVariant variant = cartItem.getProductVariant();
                
                // Ensure ProductVariant is not null
                if (variant == null) {
                    throw new RuntimeException("Product variant not found for cart item");
                }
                
                // Final stock check and reduction
                int currentStock = variant.getStockQuantity();
                if (currentStock < cartItem.getQuantity()) {
                    throw new RuntimeException("Insufficient stock for product: " + variant.getSku() + 
                        ". Available: " + currentStock + ", Required: " + cartItem.getQuantity());
                }
                
                variant.setStockQuantity(currentStock - cartItem.getQuantity());
                productVariantRepository.save(variant);
            }
        }
        
        // Save order with all cascade relationships (OrderItems will be saved automatically)
        order = orderRepository.save(order);
        
        // Clear cart
        if (request.getCartId() != null) {
            cartService.clearCart(request.getCartId());
        }
        
        // Send confirmation email (catch exceptions to not fail order creation)
        try {
            emailService.sendOrderConfirmation(order);
        } catch (Exception e) {
            log.warn("Failed to send order confirmation email: {}", e.getMessage());
        }
        
        return mapToOrderResponse(order);
    }
    
    public OrderTrackingResponse trackOrder(String trackingToken) {
        Order order = orderRepository.findByTrackingToken(trackingToken)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, 
                    "Order not found with tracking token: " + trackingToken));
        
        return mapToOrderTrackingResponse(order);
    }
    
    public String getTrackingTokenByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, 
                    "Order not found with order number: " + orderNumber));
        return order.getTrackingToken();
    }
    
    private String generateOrderNumber() {
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String random = String.format("%04d", (int) (Math.random() * 10000));
            String orderNumber = "ORD" + timestamp + random;
            
            // Check if order number already exists
            if (!orderRepository.existsByOrderNumber(orderNumber)) {
                return orderNumber;
            }
            
            // Wait a bit before retry to avoid timestamp collision
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while generating order number", e);
            }
        }
        throw new RuntimeException("Failed to generate unique order number after " + maxRetries + " attempts");
    }
    
    private String generateTrackingToken() {
        int maxRetries = 5;
        SecureRandom random = new SecureRandom();
        
        for (int i = 0; i < maxRetries; i++) {
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            String trackingToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            
            // Check if tracking token already exists
            if (!orderRepository.existsByTrackingToken(trackingToken)) {
                return trackingToken;
            }
        }
        throw new RuntimeException("Failed to generate unique tracking token after " + maxRetries + " attempts");
    }
    
    private Order createOrderWithRetry(PlaceOrderRequest request, BigDecimal totalAmount) {
        int maxRetries = 3;
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                Order order = new Order();
                order.setOrderNumber(generateOrderNumber());
                order.setTrackingToken(generateTrackingToken());
                order.setCustomerName(request.getCustomerName().trim());
                order.setCustomerPhone(request.getCustomerPhone().trim());
                order.setCustomerEmail(request.getCustomerEmail().trim());
                order.setShippingAddress(request.getShippingAddress().trim());
                order.setNote(request.getNote() != null ? request.getNote().trim() : null);
                order.setPaymentMethod(request.getPaymentMethod());
                
                // Set initial status based on payment method
                OrderStatus initialStatus = request.getPaymentMethod() == PaymentMethod.COD 
                        ? OrderStatus.CONFIRMED 
                        : OrderStatus.AWAITING_PAYMENT;
                order.setStatus(initialStatus);
                order.setTotalAmount(totalAmount);
                
                return orderRepository.save(order);
            } catch (Exception e) {
                log.warn("Attempt {} to create order failed: {}", i + 1, e.getMessage());
                if (i == maxRetries - 1) {
                    throw new RuntimeException("Failed to create order after " + maxRetries + " attempts", e);
                }
                
                // Wait before retry
                try {
                    Thread.sleep(100 * (i + 1)); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while retrying order creation", ie);
                }
            }
        }
        throw new RuntimeException("Failed to create order");
    }
    
    private void createStatusHistory(Order order, OrderStatus status, String note) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setPreviousStatus(null); // No previous status for initial creation
        history.setStatus(status);
        history.setNote(note);
        history.setChangedBy("SYSTEM"); // System-generated status
        statusHistoryRepository.save(history);
    }
    
    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getOrderItems() != null 
                ? order.getOrderItems().stream()
                    .map(item -> new OrderResponse.OrderItemResponse(
                            item.getProductName(),
                            item.getSku(),
                            item.getSize(),
                            item.getColor(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getTotalPrice()
                    ))
                    .collect(Collectors.toList())
                : new ArrayList<>();
        
        String trackingUrl = baseUrl + "/track/" + order.getTrackingToken();
        
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getTrackingToken(),
                trackingUrl,
                order.getStatus(),
                order.getPaymentMethod(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getCustomerEmail(),
                order.getShippingAddress(),
                order.getNote(),
                items
        );
    }
    
    private OrderTrackingResponse mapToOrderTrackingResponse(Order order) {
        List<OrderTrackingResponse.OrderItemSummary> items = order.getOrderItems() != null
                ? order.getOrderItems().stream()
                    .map(item -> new OrderTrackingResponse.OrderItemSummary(
                            item.getProductName(),
                            item.getSku(),
                            item.getSize(),
                            item.getColor(),
                            item.getQuantity(),
                            item.getUnitPrice()
                    ))
                    .collect(Collectors.toList())
                : new ArrayList<>();
        
        List<OrderTrackingResponse.StatusHistoryResponse> statusHistory = order.getStatusHistory() != null
                ? order.getStatusHistory().stream()
                    .map(history -> new OrderTrackingResponse.StatusHistoryResponse(
                            history.getStatus(),
                            history.getNote(),
                            history.getCreatedAt()
                    ))
                    .collect(Collectors.toList())
                : new ArrayList<>();
        
        return new OrderTrackingResponse(
                order.getOrderNumber(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getCustomerName(),
                order.getShippingAddress(),
                items,
                statusHistory
        );
    }
}