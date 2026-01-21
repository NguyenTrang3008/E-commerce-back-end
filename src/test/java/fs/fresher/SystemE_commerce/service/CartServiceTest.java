package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.request.AddCartItemRequest;
import fs.fresher.SystemE_commerce.dto.request.UpdateCartItemRequest;
import fs.fresher.SystemE_commerce.dto.response.CartResponse;
import fs.fresher.SystemE_commerce.entity.*;
import fs.fresher.SystemE_commerce.repository.CartItemRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    
    @Mock
    private CartItemRepository cartItemRepository;
    
    @Mock
    private ProductVariantRepository productVariantRepository;
    
    @Mock
    private InventoryReservationService inventoryReservationService;
    
    @Mock
    private CartCleanupService cartCleanupService;

    @InjectMocks
    private CartService cartService;

    private Cart mockCart;
    private CartItem mockCartItem;
    private ProductVariant mockVariant;
    private Product mockProduct;
    private AddCartItemRequest addItemRequest;
    private UpdateCartItemRequest updateItemRequest;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        // Mock Product
        mockProduct = new Product();
        mockProduct.setId(1L);
        mockProduct.setName("Test Product");

        // Mock ProductVariant
        mockVariant = new ProductVariant();
        mockVariant.setId(1L);
        mockVariant.setProduct(mockProduct);
        mockVariant.setSku("TEST-SKU-001");
        mockVariant.setSize("M");
        mockVariant.setColor("Red");
        mockVariant.setPrice(new BigDecimal("100.00"));
        mockVariant.setStockQuantity(10);
        mockVariant.setReservedQuantity(0);

        // Mock CartItem
        mockCartItem = new CartItem();
        mockCartItem.setId(1L);
        mockCartItem.setProductVariant(mockVariant);
        mockCartItem.setQuantity(2);

        // Mock Cart
        mockCart = new Cart();
        mockCart.setId(1L);
        mockCart.setCartId("cart-123");
        mockCart.setItems(Arrays.asList(mockCartItem));
        mockCart.setUpdatedAt(LocalDateTime.now());

        // Set up cart item relationship
        mockCartItem.setCart(mockCart);

        // Request objects
        addItemRequest = new AddCartItemRequest();
        addItemRequest.setSkuId(1L);
        addItemRequest.setQuantity(3);

        updateItemRequest = new UpdateCartItemRequest();
        updateItemRequest.setQuantity(5);
    }

    @Test
    void getCart_WithExistingCart_ShouldReturnCartResponse() {
        // Given
        when(cartRepository.findByCartId("cart-123")).thenReturn(Optional.of(mockCart));

        // When
        CartResponse result = cartService.getCart("cart-123");

        // Then
        assertNotNull(result);
        assertEquals("cart-123", result.getCartId());
        assertEquals(1, result.getItems().size());
        assertEquals(new BigDecimal("200.00"), result.getTotalAmount());
        assertEquals(2, result.getTotalItems());
        
        CartResponse.CartItemResponse item = result.getItems().get(0);
        assertEquals(1L, item.getSkuId());
        assertEquals("TEST-SKU-001", item.getSku());
        assertEquals("Test Product", item.getProductName());
        assertEquals(2, item.getQuantity());
        assertEquals(new BigDecimal("200.00"), item.getSubtotal());
    }

    @Test
    void getCart_WithNonExistingCart_ShouldCreateNewCart() {
        // Given
        Cart newCart = new Cart();
        newCart.setCartId("new-cart-123");
        newCart.setItems(Arrays.asList());
        
        when(cartRepository.findByCartId("new-cart-123")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        // When
        CartResponse result = cartService.getCart("new-cart-123");

        // Then
        assertNotNull(result);
        assertEquals("new-cart-123", result.getCartId());
        assertEquals(0, result.getItems().size());
        assertEquals(BigDecimal.ZERO, result.getTotalAmount());
        assertEquals(0, result.getTotalItems());
        
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addItemToCart_WithNewItem_ShouldAddItemSuccessfully() {
        // Given
        when(cartRepository.findByCartId("cart-123")).thenReturn(Optional.of(mockCart));
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(mockVariant));
        when(inventoryReservationService.getAvailableStock(1L)).thenReturn(10);
        when(cartItemRepository.findByCartIdAndProductVariantId(1L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(mockCartItem);
        when(cartRepository.findByCartIdWithItems("cart-123")).thenReturn(Optional.of(mockCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);

        // When
        CartResponse result = cartService.addItemToCart("cart-123", addItemRequest);

        // Then
        assertNotNull(result);
        verify(cartItemRepository).save(argThat(item -> 
            item.getQuantity() == 3 && item.getProductVariant().getId().equals(1L)
        ));
    }

    @Test
    void addItemToCart_WithExistingItem_ShouldUpdateQuantity() {
        // Given
        when(cartRepository.findByCartId("cart-123")).thenReturn(Optional.of(mockCart));
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(mockVariant));
        when(inventoryReservationService.getAvailableStock(1L)).thenReturn(10);
        when(cartItemRepository.findByCartIdAndProductVariantId(1L, 1L)).thenReturn(Optional.of(mockCartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(mockCartItem);
        when(cartRepository.findByCartIdWithItems("cart-123")).thenReturn(Optional.of(mockCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);

        // When
        CartResponse result = cartService.addItemToCart("cart-123", addItemRequest);

        // Then
        assertNotNull(result);
        verify(cartItemRepository).save(argThat(item -> 
            item.getQuantity() == 5 // 2 existing + 3 new
        ));
    }

    @Test
    void addItemToCart_WithInsufficientStock_ShouldThrowException() {
        // Given
        when(cartRepository.findByCartId("cart-123")).thenReturn(Optional.of(mockCart));
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(mockVariant));
        when(inventoryReservationService.getAvailableStock(1L)).thenReturn(2); // Less than requested

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> cartService.addItemToCart("cart-123", addItemRequest));
        assertTrue(exception.getMessage().contains("Insufficient stock"));
        
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItemToCart_WithNonExistentProduct_ShouldThrowException() {
        // Given
        when(cartRepository.findByCartId("cart-123")).thenReturn(Optional.of(mockCart));
        when(productVariantRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> cartService.addItemToCart("cart-123", addItemRequest));
        assertEquals("Product variant not found", exception.getMessage());
    }

    @Test
    void updateCartItem_WithValidQuantity_ShouldUpdateSuccessfully() {
        // Given
        when(cartRepository.findByCartId("cart-123")).thenReturn(Optional.of(mockCart));
        when(cartItemRepository.findByCartIdAndProductVariantId(1L, 1L)).thenReturn(Optional.of(mockCartItem));
        when(inventoryReservationService.getAvailableStock(1L)).thenReturn(10);
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(mockCartItem);
        when(cartRepository.findByCartIdWithItems("cart-123")).thenReturn(Optional.of(mockCart));

        // When
        CartResponse result = cartService.updateCartItem("cart-123", 1L, updateItemRequest);

        // Then
        assertNotNull(result);
        verify(cartItemRepository).save(argThat(item -> 
            item.getQuantity() == 5
        ));
    }

    @Test
    void updateCartItem_WithInsufficientStock_ShouldThrowException() {
        // Given
        when(cartRepository.findByCartId("cart-123")).thenReturn(Optional.of(mockCart));
        when(cartItemRepository.findByCartIdAndProductVariantId(1L, 1L)).thenReturn(Optional.of(mockCartItem));
        when(inventoryReservationService.getAvailableStock(1L)).thenReturn(3); // Less than requested

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> cartService.updateCartItem("cart-123", 1L, updateItemRequest));
        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }

    @Test
    void updateCartItem_WithNonExistentCart_ShouldThrowException() {
        // Given
        when(cartRepository.findByCartId("invalid-cart")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> cartService.updateCartItem("invalid-cart", 1L, updateItemRequest));
        assertEquals("Cart not found", exception.getMessage());
    }

    @Test
    void updateCartItem_WithNonExistentItem_ShouldThrowException() {
        // Given
        when(cartRepository.findByCartId("cart-123")).thenReturn(Optional.of(mockCart));
        when(cartItemRepository.findByCartIdAndProductVariantId(1L, 1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> cartService.updateCartItem("cart-123", 1L, updateItemRequest));
        assertEquals("Item not found in cart", exception.getMessage());
    }

    @Test
    void removeItemFromCart_WithValidItem_ShouldRemoveSuccessfully() {
        // Given
        when(cartRepository.findByCartId("cart-123")).thenReturn(Optional.of(mockCart));
        when(cartRepository.findByCartIdWithItems("cart-123")).thenReturn(Optional.of(mockCart));

        // When
        CartResponse result = cartService.removeItemFromCart("cart-123", 1L);

        // Then
        assertNotNull(result);
        verify(cartItemRepository).deleteByCartIdAndProductVariantId(1L, 1L);
    }

    @Test
    void removeItemFromCart_WithNonExistentCart_ShouldThrowException() {
        // Given
        when(cartRepository.findByCartId("invalid-cart")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> cartService.removeItemFromCart("invalid-cart", 1L));
        assertEquals("Cart not found", exception.getMessage());
    }

    @Test
    void clearCart_WithValidCart_ShouldClearAllItems() {
        // Given
        when(cartRepository.findByCartId("cart-123")).thenReturn(Optional.of(mockCart));

        // When
        cartService.clearCart("cart-123");

        // Then
        verify(cartItemRepository).deleteAll(mockCart.getItems());
    }

    @Test
    void clearCart_WithNonExistentCart_ShouldThrowException() {
        // Given
        when(cartRepository.findByCartId("invalid-cart")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> cartService.clearCart("invalid-cart"));
        assertEquals("Cart not found", exception.getMessage());
    }

    @Test
    void getCart_WithEmptyCart_ShouldReturnEmptyCartResponse() {
        // Given
        Cart emptyCart = new Cart();
        emptyCart.setCartId("empty-cart");
        emptyCart.setItems(Arrays.asList());
        emptyCart.setUpdatedAt(LocalDateTime.now());
        
        when(cartRepository.findByCartId("empty-cart")).thenReturn(Optional.of(emptyCart));

        // When
        CartResponse result = cartService.getCart("empty-cart");

        // Then
        assertNotNull(result);
        assertEquals("empty-cart", result.getCartId());
        assertEquals(0, result.getItems().size());
        assertEquals(BigDecimal.ZERO, result.getTotalAmount());
        assertEquals(0, result.getTotalItems());
    }

    @Test
    void addItemToCart_WithExistingItemAndInsufficientStockForTotal_ShouldThrowException() {
        // Given
        mockCartItem.setQuantity(8); // Already 8 in cart
        addItemRequest.setQuantity(3); // Want to add 3 more = 11 total
        
        when(cartRepository.findByCartId("cart-123")).thenReturn(Optional.of(mockCart));
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(mockVariant));
        when(inventoryReservationService.getAvailableStock(1L)).thenReturn(10); // Only 10 available
        when(cartItemRepository.findByCartIdAndProductVariantId(1L, 1L)).thenReturn(Optional.of(mockCartItem));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> cartService.addItemToCart("cart-123", addItemRequest));
        assertTrue(exception.getMessage().contains("Insufficient stock"));
        assertTrue(exception.getMessage().contains("Current in cart: 8"));
    }
}