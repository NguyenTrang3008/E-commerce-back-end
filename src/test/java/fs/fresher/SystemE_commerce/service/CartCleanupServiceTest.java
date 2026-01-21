package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.entity.Cart;
import fs.fresher.SystemE_commerce.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartCleanupServiceTest {
    
    @Mock
    private CartRepository cartRepository;
    
    @InjectMocks
    private CartCleanupService cartCleanupService;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cartCleanupService, "cartTtlDays", 7);
        ReflectionTestUtils.setField(cartCleanupService, "inactiveThresholdDays", 3);
    }
    
    @Test
    void cleanupExpiredCarts_WithExpiredCarts_ShouldDeleteThem() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Cart expiredCart1 = createMockCart("cart-1", now.minusDays(1));
        Cart expiredCart2 = createMockCart("cart-2", now.minusDays(2));
        
        when(cartRepository.findExpiredCarts(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(expiredCart1, expiredCart2));
        
        // When
        Map<String, Object> result = cartCleanupService.cleanupExpiredCarts();
        
        // Then
        assertEquals(2, result.get("expiredCartsFound"));
        assertEquals(2, result.get("deletedCarts"));
        assertEquals(0, result.get("errors"));
        
        verify(cartRepository).delete(expiredCart1);
        verify(cartRepository).delete(expiredCart2);
    }
    
    @Test
    void cleanupExpiredCarts_WithNoExpiredCarts_ShouldReturnZero() {
        // Given
        when(cartRepository.findExpiredCarts(any(LocalDateTime.class)))
                .thenReturn(List.of());
        
        // When
        Map<String, Object> result = cartCleanupService.cleanupExpiredCarts();
        
        // Then
        assertEquals(0, result.get("expiredCartsFound"));
        assertEquals(0, result.get("deletedCarts"));
        
        verify(cartRepository, never()).delete(any(Cart.class));
    }
    
    @Test
    void cleanupEmptyInactiveCarts_WithEmptyInactiveCarts_ShouldDeleteThem() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Cart emptyCart1 = createMockCart("empty-cart-1", now.plusDays(1));
        Cart emptyCart2 = createMockCart("empty-cart-2", now.plusDays(2));
        
        when(cartRepository.findEmptyInactiveCarts(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(emptyCart1, emptyCart2));
        
        // When
        Map<String, Object> result = cartCleanupService.cleanupEmptyInactiveCarts();
        
        // Then
        assertEquals(2, result.get("emptyInactiveCartsFound"));
        assertEquals(2, result.get("deletedCarts"));
        assertEquals(0, result.get("errors"));
        
        verify(cartRepository).delete(emptyCart1);
        verify(cartRepository).delete(emptyCart2);
    }
    
    @Test
    void getCartStatistics_ShouldReturnCorrectStats() {
        // Given
        when(cartRepository.findExpiredCarts(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(new Cart(), new Cart()));
        when(cartRepository.findInactiveCarts(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(new Cart()));
        when(cartRepository.findEmptyCarts())
                .thenReturn(Arrays.asList(new Cart(), new Cart(), new Cart()));
        when(cartRepository.count()).thenReturn(10L);
        
        // When
        Map<String, Object> stats = cartCleanupService.getCartStatistics();
        
        // Then
        assertEquals(10L, stats.get("totalCarts"));
        assertEquals(2, stats.get("expiredCarts"));
        assertEquals(1, stats.get("inactiveCarts"));
        assertEquals(3, stats.get("emptyCarts"));
        assertNotNull(stats.get("timestamp"));
    }
    
    @Test
    void cleanupCartsBySession_WithExistingCart_ShouldDeleteCart() {
        // Given
        String sessionToken = "session-123";
        Cart cart = createMockCart("cart-123", LocalDateTime.now().plusDays(1));
        
        when(cartRepository.findBySessionToken(sessionToken))
                .thenReturn(Optional.of(cart));
        
        // When
        Map<String, Object> result = cartCleanupService.cleanupCartsBySession(sessionToken);
        
        // Then
        assertEquals(1, result.get("deletedCarts"));
        assertEquals("cart-123", result.get("cartId"));
        
        verify(cartRepository).delete(cart);
    }
    
    @Test
    void cleanupCartsBySession_WithNoCart_ShouldReturnZero() {
        // Given
        String sessionToken = "session-123";
        
        when(cartRepository.findBySessionToken(sessionToken))
                .thenReturn(Optional.empty());
        
        // When
        Map<String, Object> result = cartCleanupService.cleanupCartsBySession(sessionToken);
        
        // Then
        assertEquals(0, result.get("deletedCarts"));
        assertEquals("No cart found for session", result.get("message"));
        
        verify(cartRepository, never()).delete(any(Cart.class));
    }
    
    private Cart createMockCart(String cartId, LocalDateTime expiresAt) {
        Cart cart = new Cart();
        cart.setCartId(cartId);
        cart.setExpiresAt(expiresAt);
        cart.setLastAccessedAt(LocalDateTime.now().minusDays(5));
        return cart;
    }
}