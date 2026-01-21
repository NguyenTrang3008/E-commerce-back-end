package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.request.AddCartItemRequest;
import fs.fresher.SystemE_commerce.dto.request.UpdateCartItemRequest;
import fs.fresher.SystemE_commerce.dto.response.CartResponse;
import fs.fresher.SystemE_commerce.validator.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartRequestService {
    
    private final CartService cartService;
    private final ValidationService validationService;
    
    /**
     * Resolve cart ID from multiple sources
     */
    public String resolveCartId(String requestCartId, String cookieCartId, boolean required) {
        String finalCartId = requestCartId != null ? requestCartId : cookieCartId;
        
        if (required) {
            validationService.validateCartId(finalCartId, true);
        }
        
        return finalCartId;
    }
    
    /**
     * Handle get cart request with session support
     */
    public CartResponse handleGetCart(String requestCartId, String cookieCartId, String sessionToken) {
        String finalCartId = resolveCartId(requestCartId, cookieCartId, false);
        log.debug("Getting cart with ID: {}, session: {}", finalCartId, sessionToken);
        
        if (finalCartId != null) {
            // Associate cart with session if provided
            if (sessionToken != null) {
                cartService.associateCartWithSession(finalCartId, sessionToken);
            }
            return cartService.getCart(finalCartId);
        } else if (sessionToken != null) {
            // Try to get cart by session
            try {
                return cartService.getCartBySession(sessionToken);
            } catch (RuntimeException e) {
                // No cart found for session, create new one
                return cartService.getCart(null);
            }
        } else {
            // Create new cart
            return cartService.getCart(null);
        }
    }
    
    /**
     * Handle get cart by session
     */
    public CartResponse handleGetCartBySession(String sessionToken) {
        log.debug("Getting cart by session: {}", sessionToken);
        try {
            return cartService.getCartBySession(sessionToken);
        } catch (RuntimeException e) {
            // No cart found for session, create new one
            log.debug("No cart found for session {}, creating new cart", sessionToken);
            return cartService.getCart(null);
        }
    }
    
    /**
     * Handle add item to cart request with session support
     */
    public CartResponse handleAddItemToCart(String requestCartId, String cookieCartId, AddCartItemRequest request, String sessionToken) {
        validationService.validateAddCartItemRequest(request);
        
        String finalCartId = resolveCartId(requestCartId, cookieCartId, false);
        log.info("Adding item to cart {}: sku={}, quantity={}, session={}", 
                finalCartId, request.getSkuId(), request.getQuantity(), sessionToken);
        
        return cartService.addItemToCart(finalCartId, request, sessionToken);
    }
    
    /**
     * Handle update cart item request with session support
     */
    public CartResponse handleUpdateCartItem(String requestCartId, String cookieCartId, Long variantId, UpdateCartItemRequest request, String sessionToken) {
        validationService.validateUpdateCartItemRequest(request);
        
        String finalCartId = resolveCartId(requestCartId, cookieCartId, true);
        log.info("Updating cart item in cart {}: variant={}, quantity={}, session={}", 
                finalCartId, variantId, request.getQuantity(), sessionToken);
        
        return cartService.updateCartItem(finalCartId, variantId, request);
    }
    
    /**
     * Handle remove item from cart request with session support
     */
    public CartResponse handleRemoveItemFromCart(String requestCartId, String cookieCartId, Long variantId, String sessionToken) {
        String finalCartId = resolveCartId(requestCartId, cookieCartId, true);
        log.info("Removing item from cart {}: variant={}, session={}", finalCartId, variantId, sessionToken);
        
        return cartService.removeItemFromCart(finalCartId, variantId);
    }
    
    /**
     * Handle clear cart request with session support
     */
    public void handleClearCart(String requestCartId, String cookieCartId, String sessionToken) {
        String finalCartId = resolveCartId(requestCartId, cookieCartId, true);
        log.info("Clearing cart: {}, session: {}", finalCartId, sessionToken);
        
        cartService.clearCart(finalCartId);
    }
    
    // Backward compatibility methods
    public CartResponse handleGetCart(String requestCartId, String cookieCartId) {
        return handleGetCart(requestCartId, cookieCartId, null);
    }
    
    public CartResponse handleAddItemToCart(String requestCartId, String cookieCartId, AddCartItemRequest request) {
        return handleAddItemToCart(requestCartId, cookieCartId, request, null);
    }
    
    public CartResponse handleUpdateCartItem(String requestCartId, String cookieCartId, Long variantId, UpdateCartItemRequest request) {
        return handleUpdateCartItem(requestCartId, cookieCartId, variantId, request, null);
    }
    
    public CartResponse handleRemoveItemFromCart(String requestCartId, String cookieCartId, Long variantId) {
        return handleRemoveItemFromCart(requestCartId, cookieCartId, variantId, null);
    }
    
    public void handleClearCart(String requestCartId, String cookieCartId) {
        handleClearCart(requestCartId, cookieCartId, null);
    }
}