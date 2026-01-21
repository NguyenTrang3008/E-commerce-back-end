package fs.fresher.SystemE_commerce.controller;

import fs.fresher.SystemE_commerce.dto.request.PlaceOrderRequest;
import fs.fresher.SystemE_commerce.dto.response.OrderResponse;
import fs.fresher.SystemE_commerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody PlaceOrderRequest request) {
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.ok(response);
    }
    

}