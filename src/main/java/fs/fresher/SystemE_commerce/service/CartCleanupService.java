package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.entity.Cart;
import fs.fresher.SystemE_commerce.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartCleanupService {
    
    private final CartRepository cartRepository;
    
    @Value("${cart.ttl-days:7}")
    private int cartTtlDays;
    
    @Value("${cart.inactive-threshold-days:3}")
    private int inactiveThresholdDays;
    
    /**
     * Clean up expired carts
     */
    @Transactional
    public Map<String, Object> cleanupExpiredCarts() {
        log.info("CART CLEANUP: Starting expired carts cleanup");
        
        LocalDateTime now = LocalDateTime.now();
        List<Cart> expiredCarts = cartRepository.findExpiredCarts(now);
        
        Map<String, Object> result = new HashMap<>();
        result.put("expiredCartsFound", expiredCarts.size());
        
        if (expiredCarts.isEmpty()) {
            log.debug("CART CLEANUP: No expired carts found");
            result.put("deletedCarts", 0);
            return result;
        }
        
        int deletedCount = 0;
        int errorCount = 0;
        
        for (Cart cart : expiredCarts) {
            try {
                log.debug("CART CLEANUP: Deleting expired cart: {} (expired at: {})", 
                         cart.getCartId(), cart.getExpiresAt());
                cartRepository.delete(cart);
                deletedCount++;
            } catch (Exception e) {
                errorCount++;
                log.error("CART CLEANUP: Failed to delete expired cart {}: {}", 
                         cart.getCartId(), e.getMessage());
            }
        }
        
        result.put("deletedCarts", deletedCount);
        result.put("errors", errorCount);
        
        log.info("CART CLEANUP: Expired carts cleanup completed - {} deleted, {} errors", 
                deletedCount, errorCount);
        
        return result;
    }
    
    /**
     * Clean up empty inactive carts
     */
    @Transactional
    public Map<String, Object> cleanupEmptyInactiveCarts() {
        log.info("CART CLEANUP: Starting empty inactive carts cleanup");
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(inactiveThresholdDays);
        List<Cart> emptyInactiveCarts = cartRepository.findEmptyInactiveCarts(cutoffTime);
        
        Map<String, Object> result = new HashMap<>();
        result.put("emptyInactiveCartsFound", emptyInactiveCarts.size());
        
        if (emptyInactiveCarts.isEmpty()) {
            log.debug("CART CLEANUP: No empty inactive carts found");
            result.put("deletedCarts", 0);
            return result;
        }
        
        int deletedCount = 0;
        int errorCount = 0;
        
        for (Cart cart : emptyInactiveCarts) {
            try {
                log.debug("CART CLEANUP: Deleting empty inactive cart: {} (last accessed: {})", 
                         cart.getCartId(), cart.getLastAccessedAt());
                cartRepository.delete(cart);
                deletedCount++;
            } catch (Exception e) {
                errorCount++;
                log.error("CART CLEANUP: Failed to delete empty inactive cart {}: {}", 
                         cart.getCartId(), e.getMessage());
            }
        }
        
        result.put("deletedCarts", deletedCount);
        result.put("errors", errorCount);
        
        log.info("CART CLEANUP: Empty inactive carts cleanup completed - {} deleted, {} errors", 
                deletedCount, errorCount);
        
        return result;
    }
    
    /**
     * Get cart statistics for monitoring
     */
    public Map<String, Object> getCartStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inactiveCutoff = now.minusDays(inactiveThresholdDays);
        
        List<Cart> expiredCarts = cartRepository.findExpiredCarts(now);
        List<Cart> inactiveCarts = cartRepository.findInactiveCarts(inactiveCutoff);
        List<Cart> emptyCarts = cartRepository.findEmptyCarts();
        long totalCarts = cartRepository.count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCarts", totalCarts);
        stats.put("expiredCarts", expiredCarts.size());
        stats.put("inactiveCarts", inactiveCarts.size());
        stats.put("emptyCarts", emptyCarts.size());
        stats.put("timestamp", now);
        
        return stats;
    }
    
    /**
     * Extend cart expiration
     */
    @Transactional
    public void extendCartExpiration(String cartId) {
        cartRepository.findByCartId(cartId).ifPresent(cart -> {
            cart.extendExpiration(cartTtlDays);
            cartRepository.save(cart);
            log.debug("CART CLEANUP: Extended expiration for cart: {} to {}", 
                     cartId, cart.getExpiresAt());
        });
    }
    
    /**
     * Clean up carts by session token (when session expires)
     * Note: This is mainly for cleanup purposes, normal cart operations use /api/cart endpoint
     */
    @Transactional
    public Map<String, Object> cleanupCartsBySession(String sessionToken) {
        log.info("CART CLEANUP: Cleaning up carts for expired session: {}", sessionToken);
        
        Map<String, Object> result = new HashMap<>();
        
        cartRepository.findBySessionToken(sessionToken).ifPresentOrElse(
            cart -> {
                try {
                    cartRepository.delete(cart);
                    result.put("deletedCarts", 1);
                    result.put("cartId", cart.getCartId());
                    log.info("CART CLEANUP: Deleted cart {} for expired session {}", 
                            cart.getCartId(), sessionToken);
                } catch (Exception e) {
                    result.put("deletedCarts", 0);
                    result.put("error", e.getMessage());
                    log.error("CART CLEANUP: Failed to delete cart for session {}: {}", 
                             sessionToken, e.getMessage());
                }
            },
            () -> {
                result.put("deletedCarts", 0);
                result.put("message", "No cart found for session");
                log.debug("CART CLEANUP: No cart found for session: {}", sessionToken);
            }
        );
        
        return result;
    }
}