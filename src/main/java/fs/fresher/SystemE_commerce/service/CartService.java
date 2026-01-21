package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.request.AddCartItemRequest;
import fs.fresher.SystemE_commerce.dto.request.UpdateCartItemRequest;
import fs.fresher.SystemE_commerce.dto.response.CartResponse;
import fs.fresher.SystemE_commerce.entity.Cart;
import fs.fresher.SystemE_commerce.entity.CartItem;
import fs.fresher.SystemE_commerce.entity.ProductVariant;
import fs.fresher.SystemE_commerce.repository.CartItemRepository;
import fs.fresher.SystemE_commerce.repository.CartRepository;
import fs.fresher.SystemE_commerce.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryReservationService inventoryReservationService;
    private final CartCleanupService cartCleanupService;
    
    @Value("${cart.ttl-days:7}")
    private int cartTtlDays;
    
    public CartResponse getCart(String cartId) {
        Cart cart = getOrCreateCart(cartId, null);
        // Check if cart is expired
        if (cart.isExpired()) {
            throw new RuntimeException("Cart has expired");
        }
        
        // Reload cart with items to ensure they are fetched
        cart = cartRepository.findByCartIdWithItems(cart.getCartId())
                .orElse(cart);
        return mapToCartResponse(cart);
    }
    
    public CartResponse addItemToCart(String cartId, AddCartItemRequest request) {
        return addItemToCart(cartId, request, null);
    }
    
    public CartResponse addItemToCart(String cartId, AddCartItemRequest request, String sessionToken) {
        Cart cart = getOrCreateCart(cartId, sessionToken);
        
        // Check if cart is expired
        if (cart.isExpired()) {
            throw new RuntimeException("Cart has expired");
        }
        
        ProductVariant variant = productVariantRepository.findById(request.getSkuId())
                .orElseThrow(() -> new RuntimeException("Product variant not found"));
        
        // Check available stock using inventory service
        int availableStock = inventoryReservationService.getAvailableStock(request.getSkuId());
        if (availableStock < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock. Available: " + availableStock);
        }
        
        // Check if item already exists in cart
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductVariantId(
                cart.getId(), request.getSkuId());
        
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();
            int currentAvailableStock = inventoryReservationService.getAvailableStock(request.getSkuId());
            
            if (currentAvailableStock < newQuantity) {
                throw new RuntimeException("Insufficient stock. Available: " + currentAvailableStock + 
                        ", Current in cart: " + item.getQuantity());
            }
            
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductVariant(variant);
            newItem.setQuantity(request.getQuantity());
            cartItemRepository.save(newItem);
        }
        
        // Extend cart expiration when items are added
        cart.extendExpiration(cartTtlDays);
        cartRepository.save(cart);
        
        // Refresh cart to get updated items
        cart = cartRepository.findByCartIdWithItems(cart.getCartId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        
        return mapToCartResponse(cart);
    }
    
    public CartResponse updateCartItem(String cartId, Long skuId, UpdateCartItemRequest request) {
        Cart cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        
        CartItem item = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), skuId)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));
        
        ProductVariant variant = item.getProductVariant();
        int availableStock = inventoryReservationService.getAvailableStock(variant.getId());
        
        if (availableStock < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock. Available: " + availableStock);
        }
        
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        
        // Refresh cart to get updated items
        cart = cartRepository.findByCartIdWithItems(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        
        return mapToCartResponse(cart);
    }
    
    public CartResponse removeItemFromCart(String cartId, Long skuId) {
        Cart cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        
        cartItemRepository.deleteByCartIdAndProductVariantId(cart.getId(), skuId);
        
        // Refresh cart to get updated items
        cart = cartRepository.findByCartIdWithItems(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        
        return mapToCartResponse(cart);
    }
    
    public void clearCart(String cartId) {
        Cart cart = cartRepository.findByCartId(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        
        cartItemRepository.deleteAll(cart.getItems());
    }
    
    private Cart getOrCreateCart(String cartId, String sessionToken) {
        return cartRepository.findByCartId(cartId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCartId(cartId != null ? cartId : UUID.randomUUID().toString());
                    newCart.setSessionToken(sessionToken);
                    newCart.extendExpiration(cartTtlDays);
                    return cartRepository.save(newCart);
                });
    }
    
    // Backward compatibility
    private Cart getOrCreateCart(String cartId) {
        return getOrCreateCart(cartId, null);
    }
    
    /**
     * Clean up expired cart for this cartId if exists
     */
    public void cleanupExpiredCart(String cartId) {
        cartRepository.findByCartId(cartId).ifPresent(cart -> {
            if (cart.isExpired()) {
                cartRepository.delete(cart);
            }
        });
    }
    
    /**
     * Associate cart with session token
     */
    @Transactional
    public void associateCartWithSession(String cartId, String sessionToken) {
        cartRepository.findByCartId(cartId).ifPresent(cart -> {
            cart.setSessionToken(sessionToken);
            cartRepository.save(cart);
        });
    }
    
    private CartResponse mapToCartResponse(Cart cart) {
        // Handle null or empty items list
        List<CartItem> items = cart.getItems();
        if (items == null) {
            items = List.of(); // Empty list
        }
        
        List<CartResponse.CartItemResponse> itemResponses = items.stream()
                .map(item -> {
                    ProductVariant variant = item.getProductVariant();
                    BigDecimal subtotal = variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    
                    return new CartResponse.CartItemResponse(
                            variant.getId(),
                            variant.getSku(),
                            variant.getProduct().getName(),
                            variant.getSize(),
                            variant.getColor(),
                            variant.getPrice(),
                            item.getQuantity(),
                            subtotal,
                            variant.getAvailableStock()
                    );
                })
                .collect(Collectors.toList());
        
        BigDecimal totalAmount = itemResponses.stream()
                .map(CartResponse.CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Integer totalItems = itemResponses.stream()
                .mapToInt(CartResponse.CartItemResponse::getQuantity)
                .sum();
        
        return new CartResponse(
                cart.getCartId(),
                itemResponses,
                totalAmount,
                totalItems,
                cart.getUpdatedAt()
        );
    }
}