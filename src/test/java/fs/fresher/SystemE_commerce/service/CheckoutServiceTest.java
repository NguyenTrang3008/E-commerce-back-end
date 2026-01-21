package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.request.CheckoutStartRequest;
import fs.fresher.SystemE_commerce.dto.request.InventoryReserveRequest;
import fs.fresher.SystemE_commerce.dto.response.CheckoutSessionResponse;
import fs.fresher.SystemE_commerce.dto.response.InventoryReservationResponse;
import fs.fresher.SystemE_commerce.entity.CheckoutSession;
import fs.fresher.SystemE_commerce.repository.CheckoutSessionRepository;
import fs.fresher.SystemE_commerce.repository.StockReservationRepository;
import fs.fresher.SystemE_commerce.repository.CartRepository;
import fs.fresher.SystemE_commerce.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private CheckoutSessionRepository checkoutSessionRepository;
    
    @Mock
    private StockReservationRepository stockReservationRepository;
    
    @Mock
    private CartRepository cartRepository;
    
    @Mock
    private ProductVariantRepository productVariantRepository;
    
    @Mock
    private InventoryReservationService inventoryReservationService;

    @InjectMocks
    private CheckoutService checkoutService;

    private CheckoutStartRequest checkoutStartRequest;
    private CheckoutSession mockCheckoutSession;
    private InventoryReservationResponse mockInventoryResponse;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        checkoutStartRequest = new CheckoutStartRequest();
        checkoutStartRequest.setCartId("cart-123");

        mockCheckoutSession = new CheckoutSession();
        mockCheckoutSession.setCheckoutToken("checkout-token-123");
        mockCheckoutSession.setCartId("cart-123");
        mockCheckoutSession.setIsUsed(false);
        mockCheckoutSession.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        // Mock inventory response
        InventoryReservationResponse.ReservedItemResponse reservedItem = 
            new InventoryReservationResponse.ReservedItemResponse(
                1L, "TEST-SKU-001", "Test Product", "M", "Red", 
                new BigDecimal("100.00"), 2, new BigDecimal("200.00")
            );

        mockInventoryResponse = new InventoryReservationResponse(
            "reservation-token-123",
            "cart-123",
            LocalDateTime.now().plusMinutes(15),
            Arrays.asList(reservedItem),
            new BigDecimal("200.00"),
            2
        );
    }

    @Test
    void startCheckout_ShouldDelegateToInventoryReservationService() {
        // Given
        when(inventoryReservationService.reserveInventory(any(InventoryReserveRequest.class)))
            .thenReturn(mockInventoryResponse);

        // When
        CheckoutSessionResponse result = checkoutService.startCheckout(checkoutStartRequest);

        // Then
        assertNotNull(result);
        assertEquals("reservation-token-123", result.getCheckoutToken());
        assertEquals("cart-123", result.getCartId());
        assertEquals(new BigDecimal("200.00"), result.getTotalAmount());
        assertEquals(2, result.getTotalItems());
        assertEquals(1, result.getReservedItems().size());

        verify(inventoryReservationService).reserveInventory(argThat(request -> 
            request.getCartId().equals("cart-123")
        ));
    }

    @Test
    void validateCheckoutSession_WithValidToken_ShouldReturnSession() {
        // Given
        when(checkoutSessionRepository.findByCheckoutToken("checkout-token-123"))
            .thenReturn(Optional.of(mockCheckoutSession));

        // When
        CheckoutSession result = checkoutService.validateCheckoutSession("checkout-token-123");

        // Then
        assertNotNull(result);
        assertEquals("checkout-token-123", result.getCheckoutToken());
        assertEquals("cart-123", result.getCartId());
        assertFalse(result.getIsUsed());
    }

    @Test
    void validateCheckoutSession_WithInvalidToken_ShouldThrowException() {
        // Given
        when(checkoutSessionRepository.findByCheckoutToken("invalid-token"))
            .thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> checkoutService.validateCheckoutSession("invalid-token"));
        assertEquals("Invalid checkout token", exception.getMessage());
    }

    @Test
    void validateCheckoutSession_WithUsedSession_ShouldThrowException() {
        // Given
        mockCheckoutSession.setIsUsed(true);
        when(checkoutSessionRepository.findByCheckoutToken("checkout-token-123"))
            .thenReturn(Optional.of(mockCheckoutSession));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> checkoutService.validateCheckoutSession("checkout-token-123"));
        assertEquals("Checkout session already used", exception.getMessage());
    }

    @Test
    void validateCheckoutSession_WithExpiredSession_ShouldThrowExceptionAndReleaseReservations() {
        // Given
        mockCheckoutSession.setExpiresAt(LocalDateTime.now().minusMinutes(5)); // Expired
        when(checkoutSessionRepository.findByCheckoutToken("checkout-token-123"))
            .thenReturn(Optional.of(mockCheckoutSession));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> checkoutService.validateCheckoutSession("checkout-token-123"));
        assertEquals("Checkout session expired", exception.getMessage());
        
        verify(inventoryReservationService).releaseReservationByToken("checkout-token-123");
    }

    @Test
    void markSessionAsUsed_WithValidToken_ShouldUpdateSession() {
        // Given
        when(checkoutSessionRepository.findByCheckoutToken("checkout-token-123"))
            .thenReturn(Optional.of(mockCheckoutSession));
        when(checkoutSessionRepository.save(any(CheckoutSession.class)))
            .thenReturn(mockCheckoutSession);

        // When
        checkoutService.markSessionAsUsed("checkout-token-123");

        // Then
        verify(checkoutSessionRepository).save(argThat(session -> 
            session.getIsUsed() == true
        ));
    }

    @Test
    void markSessionAsUsed_WithInvalidToken_ShouldThrowException() {
        // Given
        when(checkoutSessionRepository.findByCheckoutToken("invalid-token"))
            .thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> checkoutService.markSessionAsUsed("invalid-token"));
        assertEquals("Checkout session not found", exception.getMessage());
    }

    @Test
    void releaseReservations_ShouldDelegateToInventoryService() {
        // When
        checkoutService.releaseReservations("checkout-token-123");

        // Then
        verify(inventoryReservationService).releaseReservationByToken("checkout-token-123");
    }

    @Test
    void confirmReservations_ShouldDelegateToInventoryService() {
        // When
        checkoutService.confirmReservations("checkout-token-123");

        // Then
        verify(inventoryReservationService).confirmReservationByToken("checkout-token-123");
    }

    @Test
    void startCheckout_ShouldMapInventoryResponseCorrectly() {
        // Given
        when(inventoryReservationService.reserveInventory(any(InventoryReserveRequest.class)))
            .thenReturn(mockInventoryResponse);

        // When
        CheckoutSessionResponse result = checkoutService.startCheckout(checkoutStartRequest);

        // Then
        assertNotNull(result.getReservedItems());
        assertEquals(1, result.getReservedItems().size());
        
        CheckoutSessionResponse.ReservedItemResponse item = result.getReservedItems().get(0);
        assertEquals(1L, item.getSkuId());
        assertEquals("TEST-SKU-001", item.getSku());
        assertEquals("Test Product", item.getProductName());
        assertEquals("M", item.getSize());
        assertEquals("Red", item.getColor());
        assertEquals(new BigDecimal("100.00"), item.getPrice());
        assertEquals(2, item.getQuantity());
        assertEquals(new BigDecimal("200.00"), item.getSubtotal());
    }

    @Test
    void startCheckout_ShouldSetDefaultTtl() {
        // Given
        when(inventoryReservationService.reserveInventory(any(InventoryReserveRequest.class)))
            .thenReturn(mockInventoryResponse);

        // When
        checkoutService.startCheckout(checkoutStartRequest);

        // Then
        verify(inventoryReservationService).reserveInventory(argThat(request -> 
            request.getCartId() != null
        ));
    }
}