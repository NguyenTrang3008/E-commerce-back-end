package fs.fresher.SystemE_commerce.controller;

import fs.fresher.SystemE_commerce.dto.request.AddCartItemRequest;
import fs.fresher.SystemE_commerce.dto.request.UpdateCartItemRequest;
import fs.fresher.SystemE_commerce.dto.response.CartResponse;
import fs.fresher.SystemE_commerce.service.CartRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    
    private final CartRequestService cartRequestService;
    
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @RequestParam(required = false) String cartId,
            @CookieValue(value = "cartId", required = false) String cookieCartId,
            HttpServletRequest request) {
        
        HttpSession session = request.getSession(false);
        String sessionToken = session != null ? session.getId() : null;
        
        CartResponse response = cartRequestService.handleGetCart(cartId, cookieCartId, sessionToken);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(
            @RequestParam(required = false) String cartId,
            @CookieValue(value = "cartId", required = false) String cookieCartId,
            @RequestBody AddCartItemRequest request,
            HttpServletRequest httpRequest) {
        
        HttpSession session = httpRequest.getSession(true); // Create session if not exists
        String sessionToken = session.getId();
        
        CartResponse response = cartRequestService.handleAddItemToCart(cartId, cookieCartId, request, sessionToken);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/items/{variantId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @RequestParam(required = false) String cartId,
            @CookieValue(value = "cartId", required = false) String cookieCartId,
            @PathVariable Long variantId,
            @RequestBody UpdateCartItemRequest request,
            HttpServletRequest httpRequest) {
        
        HttpSession session = httpRequest.getSession(false);
        String sessionToken = session != null ? session.getId() : null;
        
        CartResponse response = cartRequestService.handleUpdateCartItem(cartId, cookieCartId, variantId, request, sessionToken);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<CartResponse> removeItemFromCart(
            @RequestParam(required = false) String cartId,
            @CookieValue(value = "cartId", required = false) String cookieCartId,
            @PathVariable Long variantId,
            HttpServletRequest request) {
        
        HttpSession session = request.getSession(false);
        String sessionToken = session != null ? session.getId() : null;
        
        CartResponse response = cartRequestService.handleRemoveItemFromCart(cartId, cookieCartId, variantId, sessionToken);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @RequestParam(required = false) String cartId,
            @CookieValue(value = "cartId", required = false) String cookieCartId,
            HttpServletRequest request) {
        
        HttpSession session = request.getSession(false);
        String sessionToken = session != null ? session.getId() : null;
        
        cartRequestService.handleClearCart(cartId, cookieCartId, sessionToken);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/session")
    public ResponseEntity<CartResponse> getCartBySession(HttpServletRequest request) {
        HttpSession session = request.getSession(true); // Create session if not exists
        
        CartResponse response = cartRequestService.handleGetCartBySession(session.getId());
        return ResponseEntity.ok(response);
    }
}