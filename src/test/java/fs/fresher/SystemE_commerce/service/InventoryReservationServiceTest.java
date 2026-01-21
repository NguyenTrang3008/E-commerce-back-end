package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.request.InventoryReserveRequest;
import fs.fresher.SystemE_commerce.dto.response.InventoryReservationResponse;
import fs.fresher.SystemE_commerce.entity.*;
import fs.fresher.SystemE_commerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryReservationServiceTest {

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private CheckoutSessionRepository checkoutSessionRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private InventoryReservationService inventoryReservationService;

    private ProductVariant mockVariant;
    private Product mockProduct;
    private Cart mockCart;
    private CartItem mockCartItem;
    private CheckoutSession mockCheckoutSession;
    private StockReservation mockStockReservation;
    private InventoryReserveRequest reserveRequest;

    @BeforeEach
    void setUp() {
        // Set up configuration
        ReflectionTestUtils.setField(inventoryReservationService, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(inventoryReservationService, "retryDelayMs", 100);
        
        setupTestData();
    }

    private void setupTestData() {
        // Setup Product
        mockProduct = new Product();
        mockProduct.setId(1L);
        mockProduct.setName("Test Product");

        // Setup ProductVariant
        mockVariant = new ProductVariant();
        mockVariant.setId(1L);
        mockVariant.setProduct(mockProduct);
        mockVariant.setSku("TEST-SKU-001");
        mockVariant.setSize("M");
        mockVariant.setColor("Red");
        mockVariant.setPrice(new BigDecimal("100.00"));
        mockVariant.setStockQuantity(10);
        mockVariant.setReservedQuantity(2);

        // Setup CartItem
        mockCartItem = new CartItem();
        mockCartItem.setId(1L);
        mockCartItem.setProductVariant(mockVariant);
        mockCartItem.setQuantity(3);

        // Setup Cart
        mockCart = new Cart();
        mockCart.setId(1L);
        mockCart.setCartId("test-cart-001");
        mockCart.setItems(Arrays.asList(mockCartItem));

        // Setup CheckoutSession
        mockCheckoutSession = new CheckoutSession();
        mockCheckoutSession.setId(1L);
        mockCheckoutSession.setCheckoutToken("checkout-token-123");
        mockCheckoutSession.setCartId("test-cart-001");
        mockCheckoutSession.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        mockCheckoutSession.setIsUsed(false);

        // Setup StockReservation
        mockStockReservation = new StockReservation();
        mockStockReservation.setId(1L);
        mockStockReservation.setCheckoutSession(mockCheckoutSession);
        mockStockReservation.setProductVariant(mockVariant);
        mockStockReservation.setQuantity(3);

        // Setup Reserve Request
        reserveRequest = new InventoryReserveRequest();
        reserveRequest.setCartId("test-cart-001");
        // TTL is now fixed at 15 minutes and managed by backend
    }

    @Test
    void reserveStock_WithSufficientStock_ShouldReturnTrue() {
        // Given
        when(productVariantRepository.findByIdWithLock(1L))
                .thenReturn(Optional.of(mockVariant));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenReturn(mockVariant);

        // When
        boolean result = inventoryReservationService.reserveStock(1L, 3);

        // Then
        assertTrue(result);
        verify(productVariantRepository).findByIdWithLock(1L);
        verify(productVariantRepository).save(mockVariant);
        assertEquals(5, mockVariant.getReservedQuantity()); // 2 + 3
    }

    @Test
    void reserveStock_WithInsufficientStock_ShouldReturnFalse() {
        // Given - Available stock is 8 (10 - 2), requesting 10
        when(productVariantRepository.findByIdWithLock(1L))
                .thenReturn(Optional.of(mockVariant));

        // When
        boolean result = inventoryReservationService.reserveStock(1L, 10);

        // Then
        assertFalse(result);
        verify(productVariantRepository).findByIdWithLock(1L);
        verify(productVariantRepository, never()).save(any());
        assertEquals(2, mockVariant.getReservedQuantity()); // Unchanged
    }

    @Test
    void reserveStock_WithNonExistentVariant_ShouldThrowException() {
        // Given
        when(productVariantRepository.findByIdWithLock(999L))
                .thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            inventoryReservationService.reserveStock(999L, 3);
        });

        assertTrue(exception.getMessage().contains("Product variant not found: 999"));
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void reserveStock_WithOptimisticLockingFailure_ShouldRetryAndSucceed() {
        // Given
        when(productVariantRepository.findByIdWithLock(1L))
                .thenThrow(new OptimisticLockingFailureException("Lock failure"))
                .thenReturn(Optional.of(mockVariant));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenReturn(mockVariant);

        // When
        boolean result = inventoryReservationService.reserveStock(1L, 3);

        // Then
        assertTrue(result);
        verify(productVariantRepository, times(2)).findByIdWithLock(1L);
        verify(productVariantRepository).save(mockVariant);
    }

    @Test
    void reserveMultipleStock_WithSufficientStock_ShouldReturnTrue() {
        // Given
        Map<Long, Integer> reservationMap = new HashMap<>();
        reservationMap.put(1L, 3);
        
        when(productVariantRepository.findByIdWithLock(1L))
                .thenReturn(Optional.of(mockVariant));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenReturn(mockVariant);

        // When
        boolean result = inventoryReservationService.reserveMultipleStock(reservationMap);

        // Then
        assertTrue(result);
        verify(productVariantRepository).findByIdWithLock(1L);
        verify(productVariantRepository).save(mockVariant);
        assertEquals(5, mockVariant.getReservedQuantity());
    }

    @Test
    void reserveMultipleStock_WithInsufficientStockForOneItem_ShouldReturnFalse() {
        // Given
        ProductVariant variant2 = new ProductVariant();
        variant2.setId(2L);
        variant2.setSku("TEST-SKU-002");
        variant2.setStockQuantity(5);
        variant2.setReservedQuantity(4); // Only 1 available

        Map<Long, Integer> reservationMap = new HashMap<>();
        reservationMap.put(1L, 3); // Available: 8, requesting 3 - OK
        reservationMap.put(2L, 2); // Available: 1, requesting 2 - FAIL

        when(productVariantRepository.findByIdWithLock(1L))
                .thenReturn(Optional.of(mockVariant));
        when(productVariantRepository.findByIdWithLock(2L))
                .thenReturn(Optional.of(variant2));

        // When
        boolean result = inventoryReservationService.reserveMultipleStock(reservationMap);

        // Then
        assertFalse(result);
        verify(productVariantRepository).findByIdWithLock(1L);
        verify(productVariantRepository).findByIdWithLock(2L);
        verify(productVariantRepository, never()).save(any()); // No saves due to failure
        assertEquals(2, mockVariant.getReservedQuantity()); // Unchanged
        assertEquals(4, variant2.getReservedQuantity()); // Unchanged
    }

    @Test
    void releaseReservation_WithValidVariant_ShouldDecreaseReservedQuantity() {
        // Given
        when(productVariantRepository.findById(1L))
                .thenReturn(Optional.of(mockVariant));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenReturn(mockVariant);

        // When
        inventoryReservationService.releaseReservation(1L, 1);

        // Then
        verify(productVariantRepository).findById(1L);
        verify(productVariantRepository).save(mockVariant);
        assertEquals(1, mockVariant.getReservedQuantity()); // 2 - 1
    }

    @Test
    void releaseReservation_WithQuantityGreaterThanReserved_ShouldSetToZero() {
        // Given
        when(productVariantRepository.findById(1L))
                .thenReturn(Optional.of(mockVariant));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenReturn(mockVariant);

        // When
        inventoryReservationService.releaseReservation(1L, 5); // More than reserved (2)

        // Then
        verify(productVariantRepository).save(mockVariant);
        assertEquals(0, mockVariant.getReservedQuantity()); // Max(0, 2-5) = 0
    }

    @Test
    void confirmReservation_WithValidVariant_ShouldDecreaseStockAndReserved() {
        // Given
        when(productVariantRepository.findById(1L))
                .thenReturn(Optional.of(mockVariant));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenReturn(mockVariant);

        // When
        inventoryReservationService.confirmReservation(1L, 2);

        // Then
        verify(productVariantRepository).findById(1L);
        verify(productVariantRepository).save(mockVariant);
        assertEquals(8, mockVariant.getStockQuantity()); // 10 - 2
        assertEquals(0, mockVariant.getReservedQuantity()); // Max(0, 2-2) = 0
    }

    @Test
    void getAvailableStock_WithValidVariant_ShouldReturnCorrectAmount() {
        // Given
        when(productVariantRepository.findById(1L))
                .thenReturn(Optional.of(mockVariant));

        // When
        int availableStock = inventoryReservationService.getAvailableStock(1L);

        // Then
        assertEquals(8, availableStock); // 10 - 2
        verify(productVariantRepository).findById(1L);
    }

    @Test
    void getStockInfo_WithValidVariant_ShouldReturnStockInfo() {
        // Given
        when(productVariantRepository.findById(1L))
                .thenReturn(Optional.of(mockVariant));

        // When
        InventoryReservationService.StockInfo stockInfo = inventoryReservationService.getStockInfo(1L);

        // Then
        assertNotNull(stockInfo);
        assertEquals(1L, stockInfo.variantId);
        assertEquals("TEST-SKU-001", stockInfo.sku);
        assertEquals(10, stockInfo.totalStock);
        assertEquals(2, stockInfo.reservedStock);
        assertEquals(8, stockInfo.availableStock);
    }

    @Test
    void reserveInventory_WithValidCart_ShouldCreateReservation() {
        // Given
        when(cartRepository.findByCartId("test-cart-001"))
                .thenReturn(Optional.of(mockCart));
        when(productVariantRepository.findByIdWithLock(1L))
                .thenReturn(Optional.of(mockVariant));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenReturn(mockVariant);
        when(checkoutSessionRepository.save(any(CheckoutSession.class)))
                .thenReturn(mockCheckoutSession);
        when(stockReservationRepository.save(any(StockReservation.class)))
                .thenReturn(mockStockReservation);

        // When
        InventoryReservationResponse result = inventoryReservationService.reserveInventory(reserveRequest);

        // Then
        assertNotNull(result);
        assertEquals("checkout-token-123", result.getReservationToken());
        assertEquals("test-cart-001", result.getCartId());
        assertEquals(1, result.getReservedItems().size());
        assertEquals(new BigDecimal("300.00"), result.getTotalAmount()); // 3 * 100.00
        assertEquals(3, result.getTotalItems());

        verify(cartRepository).findByCartId("test-cart-001");
        verify(productVariantRepository).findByIdWithLock(1L);
        verify(checkoutSessionRepository).save(any(CheckoutSession.class));
        verify(stockReservationRepository).save(any(StockReservation.class));
    }

    @Test
    void reserveInventory_WithEmptyCart_ShouldThrowException() {
        // Given
        Cart emptyCart = new Cart();
        emptyCart.setCartId("empty-cart");
        emptyCart.setItems(new ArrayList<>());

        reserveRequest.setCartId("empty-cart");
        when(cartRepository.findByCartId("empty-cart"))
                .thenReturn(Optional.of(emptyCart));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            inventoryReservationService.reserveInventory(reserveRequest);
        });

        assertTrue(exception.getMessage().contains("Cart is empty"));
        verify(productVariantRepository, never()).findByIdWithLock(any());
    }

    @Test
    void reserveInventory_WithNonExistentCart_ShouldThrowException() {
        // Given
        when(cartRepository.findByCartId("non-existent-cart"))
                .thenReturn(Optional.empty());

        reserveRequest.setCartId("non-existent-cart");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            inventoryReservationService.reserveInventory(reserveRequest);
        });

        assertTrue(exception.getMessage().contains("Cart not found: non-existent-cart"));
    }

    @Test
    void releaseReservationByToken_WithValidToken_ShouldReleaseReservation() {
        // Given
        when(checkoutSessionRepository.findByCheckoutToken("checkout-token-123"))
                .thenReturn(Optional.of(mockCheckoutSession));
        when(stockReservationRepository.findByCheckoutSessionId(1L))
                .thenReturn(Arrays.asList(mockStockReservation));
        when(productVariantRepository.findById(1L))
                .thenReturn(Optional.of(mockVariant));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenReturn(mockVariant);
        when(checkoutSessionRepository.save(any(CheckoutSession.class)))
                .thenReturn(mockCheckoutSession);

        // When
        Map<String, Object> result = inventoryReservationService.releaseReservationByToken("checkout-token-123");

        // Then
        assertNotNull(result);
        assertEquals("checkout-token-123", result.get("reservationToken"));
        assertEquals("RELEASED", result.get("status"));
        assertEquals(1, result.get("releasedVariants"));

        verify(checkoutSessionRepository).findByCheckoutToken("checkout-token-123");
        verify(stockReservationRepository).findByCheckoutSessionId(1L);
        verify(productVariantRepository).findById(1L);
        assertTrue(mockCheckoutSession.getIsUsed());
    }

    @Test
    void releaseReservationByToken_WithAlreadyUsedSession_ShouldReturnAlreadyProcessed() {
        // Given
        mockCheckoutSession.setIsUsed(true);
        when(checkoutSessionRepository.findByCheckoutToken("checkout-token-123"))
                .thenReturn(Optional.of(mockCheckoutSession));

        // When
        Map<String, Object> result = inventoryReservationService.releaseReservationByToken("checkout-token-123");

        // Then
        assertNotNull(result);
        assertEquals("checkout-token-123", result.get("reservationToken"));
        assertEquals("ALREADY_PROCESSED", result.get("status"));

        verify(stockReservationRepository, never()).findByCheckoutSessionId(any());
        verify(productVariantRepository, never()).findById(any());
    }

    @Test
    void confirmReservationByToken_WithValidToken_ShouldConfirmReservation() {
        // Given
        when(checkoutSessionRepository.findByCheckoutToken("checkout-token-123"))
                .thenReturn(Optional.of(mockCheckoutSession));
        when(stockReservationRepository.findByCheckoutSessionId(1L))
                .thenReturn(Arrays.asList(mockStockReservation));
        when(productVariantRepository.findById(1L))
                .thenReturn(Optional.of(mockVariant));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenReturn(mockVariant);

        // When
        Map<String, Object> result = inventoryReservationService.confirmReservationByToken("checkout-token-123");

        // Then
        assertNotNull(result);
        assertEquals("checkout-token-123", result.get("reservationToken"));
        assertEquals("CONFIRMED", result.get("status"));

        verify(checkoutSessionRepository).findByCheckoutToken("checkout-token-123");
        verify(stockReservationRepository).findByCheckoutSessionId(1L);
        verify(productVariantRepository).findById(1L);
        verify(productVariantRepository).save(mockVariant);
    }

    @Test
    void getReservationStatusByToken_WithValidToken_ShouldReturnStatus() {
        // Given
        when(checkoutSessionRepository.findByCheckoutToken("checkout-token-123"))
                .thenReturn(Optional.of(mockCheckoutSession));

        // When
        Map<String, Object> result = inventoryReservationService.getReservationStatusByToken("checkout-token-123");

        // Then
        assertNotNull(result);
        assertEquals("checkout-token-123", result.get("reservationToken"));
        assertEquals("test-cart-001", result.get("cartId"));
        assertEquals("ACTIVE", result.get("status"));
        assertFalse((Boolean) result.get("isExpired"));
        assertFalse((Boolean) result.get("isUsed"));

        verify(checkoutSessionRepository).findByCheckoutToken("checkout-token-123");
    }

    @Test
    void getReservationStatusByToken_WithExpiredSession_ShouldReturnExpiredStatus() {
        // Given
        mockCheckoutSession.setExpiresAt(LocalDateTime.now().minusMinutes(5)); // Expired
        when(checkoutSessionRepository.findByCheckoutToken("checkout-token-123"))
                .thenReturn(Optional.of(mockCheckoutSession));

        // When
        Map<String, Object> result = inventoryReservationService.getReservationStatusByToken("checkout-token-123");

        // Then
        assertEquals("EXPIRED", result.get("status"));
        assertTrue((Boolean) result.get("isExpired"));
    }

    @Test
    void releaseMultipleReservations_WithMultipleVariants_ShouldReleaseAll() {
        // Given
        ProductVariant variant2 = new ProductVariant();
        variant2.setId(2L);
        variant2.setStockQuantity(15);
        variant2.setReservedQuantity(5);

        Map<Long, Integer> reservationMap = new HashMap<>();
        reservationMap.put(1L, 2);
        reservationMap.put(2L, 3);

        when(productVariantRepository.findById(1L))
                .thenReturn(Optional.of(mockVariant));
        when(productVariantRepository.findById(2L))
                .thenReturn(Optional.of(variant2));
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenReturn(mockVariant, variant2);

        // When
        inventoryReservationService.releaseMultipleReservations(reservationMap);

        // Then
        verify(productVariantRepository).findById(1L);
        verify(productVariantRepository).findById(2L);
        verify(productVariantRepository, times(2)).save(any(ProductVariant.class));
        assertEquals(0, mockVariant.getReservedQuantity()); // 2 - 2
        assertEquals(2, variant2.getReservedQuantity()); // 5 - 3
    }

    @Test
    void reserveStock_WithMaxRetryAttemptsExceeded_ShouldReturnFalse() {
        // Given
        when(productVariantRepository.findByIdWithLock(1L))
                .thenThrow(new OptimisticLockingFailureException("Lock failure"));

        // When
        boolean result = inventoryReservationService.reserveStock(1L, 3);

        // Then
        assertFalse(result);
        verify(productVariantRepository, times(3)).findByIdWithLock(1L); // maxRetryAttempts = 3
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void reserveInventory_WithInsufficientStock_ShouldThrowException() {
        // Given
        mockVariant.setStockQuantity(5);
        mockVariant.setReservedQuantity(4); // Only 1 available
        mockCartItem.setQuantity(2); // Requesting 2

        when(cartRepository.findByCartId("test-cart-001"))
                .thenReturn(Optional.of(mockCart));
        when(productVariantRepository.findByIdWithLock(1L))
                .thenReturn(Optional.of(mockVariant));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            inventoryReservationService.reserveInventory(reserveRequest);
        });

        assertTrue(exception.getMessage().contains("insufficient inventory"));
        verify(checkoutSessionRepository, never()).save(any());
        verify(stockReservationRepository, never()).save(any());
    }
}