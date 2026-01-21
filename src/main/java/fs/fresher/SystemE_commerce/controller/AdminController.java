package fs.fresher.SystemE_commerce.controller;

import fs.fresher.SystemE_commerce.dto.request.UpdateOrderStatusRequest;
import fs.fresher.SystemE_commerce.dto.response.AdminOrderDetailResponse;
import fs.fresher.SystemE_commerce.dto.response.AdminOrderListResponse;
import fs.fresher.SystemE_commerce.dto.response.PagedResponse;
import fs.fresher.SystemE_commerce.enums.OrderStatus;
import fs.fresher.SystemE_commerce.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final AdminOrderService adminOrderService;
    
    @GetMapping("/orders")
    public ResponseEntity<PagedResponse<AdminOrderListResponse>> getOrders(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PagedResponse<AdminOrderListResponse> response = adminOrderService.getOrders(apiKey, status, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<AdminOrderDetailResponse> getOrderDetail(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @PathVariable Long orderId) {
        
        AdminOrderDetailResponse response = adminOrderService.getOrderDetail(apiKey, orderId);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<AdminOrderDetailResponse> updateOrderStatus(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request) {
        
        AdminOrderDetailResponse response = adminOrderService.updateOrderStatus(apiKey, orderId, request);
        return ResponseEntity.ok(response);
    }
}