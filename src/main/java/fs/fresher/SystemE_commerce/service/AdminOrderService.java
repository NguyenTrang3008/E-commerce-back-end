package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.request.UpdateOrderStatusRequest;
import fs.fresher.SystemE_commerce.dto.response.AdminOrderDetailResponse;
import fs.fresher.SystemE_commerce.dto.response.AdminOrderListResponse;
import fs.fresher.SystemE_commerce.dto.response.PagedResponse;
import fs.fresher.SystemE_commerce.entity.AdminUser;
import fs.fresher.SystemE_commerce.entity.Order;
import fs.fresher.SystemE_commerce.entity.OrderStatusHistory;
import fs.fresher.SystemE_commerce.enums.OrderStatus;
import fs.fresher.SystemE_commerce.exception.BusinessException;
import fs.fresher.SystemE_commerce.exception.ErrorCode;
import fs.fresher.SystemE_commerce.exception.ResourceNotFoundException;
import fs.fresher.SystemE_commerce.exception.ValidationException;
import fs.fresher.SystemE_commerce.repository.OrderRepository;
import fs.fresher.SystemE_commerce.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminOrderService {
    
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final OrderStateMachineService stateMachineService;
    private final AdminAuthService adminAuthService;
    
    public PagedResponse<AdminOrderListResponse> getOrders(String apiKey, OrderStatus status, int page, int size) {
        // Authentication
        AdminUser admin = adminAuthService.authenticateByApiKey(apiKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid API key"));
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.findOrdersWithStatus(status, pageable);
        
        List<AdminOrderListResponse> content = orderPage.getContent().stream()
                .map(this::mapToAdminOrderListResponse)
                .collect(Collectors.toList());
        
        return new PagedResponse<>(
                content,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isFirst(),
                orderPage.isLast(),
                orderPage.hasNext(),
                orderPage.hasPrevious()
        );
    }
    
    public AdminOrderDetailResponse getOrderDetail(String apiKey, Long orderId) {
        // Authentication
        AdminUser admin = adminAuthService.authenticateByApiKey(apiKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid API key"));
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, 
                    "Order not found with ID: " + orderId));
        
        return mapToAdminOrderDetailResponse(order);
    }
    
    public AdminOrderDetailResponse updateOrderStatus(String apiKey, Long orderId, UpdateOrderStatusRequest request) {
        // Authentication
        AdminUser admin = adminAuthService.authenticateByApiKey(apiKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid API key"));
        
        // Validation
        if (request.getStatus() == null) {
            throw new ValidationException("Status is required");
        }
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, 
                    "Order not found with ID: " + orderId));
        
        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();
        
        // Validate transition
        stateMachineService.validateTransition(currentStatus, newStatus, order.getPaymentMethod());
        
        // Update order status
        order.setStatus(newStatus);
        orderRepository.save(order);
        
        // Create status history record
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setPreviousStatus(currentStatus);
        history.setStatus(newStatus);
        history.setNote(request.getNote());
        history.setChangedBy(admin.getUsername());
        statusHistoryRepository.save(history);
        
        return mapToAdminOrderDetailResponse(order);
    }
    
    // Overload for system calls (webhooks, scheduled tasks, etc.)
    public AdminOrderDetailResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, String systemUser) {
        // Validation
        if (request.getStatus() == null) {
            throw new ValidationException("Status is required");
        }
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, 
                    "Order not found with ID: " + orderId));
        
        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();
        
        // Validate transition
        stateMachineService.validateTransition(currentStatus, newStatus, order.getPaymentMethod());
        
        // Update order status
        order.setStatus(newStatus);
        orderRepository.save(order);
        
        // Create status history record
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setPreviousStatus(currentStatus);
        history.setStatus(newStatus);
        history.setNote(request.getNote());
        history.setChangedBy(systemUser);
        statusHistoryRepository.save(history);
        
        return mapToAdminOrderDetailResponse(order);
    }
    
    private AdminOrderListResponse mapToAdminOrderListResponse(Order order) {
        int totalItems = order.getOrderItems().stream()
                .mapToInt(item -> item.getQuantity())
                .sum();
        
        return new AdminOrderListResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getTotalAmount(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getCustomerEmail(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                totalItems
        );
    }
    
    private AdminOrderDetailResponse mapToAdminOrderDetailResponse(Order order) {
        List<AdminOrderDetailResponse.AdminOrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> new AdminOrderDetailResponse.AdminOrderItemResponse(
                        item.getProductName(),
                        item.getSku(),
                        item.getSize(),
                        item.getColor(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getTotalPrice()
                ))
                .collect(Collectors.toList());
        
        List<AdminOrderDetailResponse.AdminStatusHistoryResponse> statusHistory = order.getStatusHistory().stream()
                .map(history -> new AdminOrderDetailResponse.AdminStatusHistoryResponse(
                        history.getPreviousStatus(),
                        history.getStatus(),
                        history.getNote(),
                        history.getChangedBy(),
                        history.getCreatedAt()
                ))
                .collect(Collectors.toList());
        
        List<OrderStatus> availableTransitions = stateMachineService.getValidTransitions(order.getStatus());
        
        return new AdminOrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getTrackingToken(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getCustomerEmail(),
                order.getShippingAddress(),
                order.getNote(),
                items,
                statusHistory,
                availableTransitions
        );
    }
}