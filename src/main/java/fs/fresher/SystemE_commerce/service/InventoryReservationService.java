package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.request.InventoryReserveRequest;
import fs.fresher.SystemE_commerce.dto.response.InventoryReservationResponse;
import fs.fresher.SystemE_commerce.entity.*;
import fs.fresher.SystemE_commerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationService {
    
    private final ProductVariantRepository productVariantRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final StockReservationRepository stockReservationRepository;
    private final CartRepository cartRepository;
    
    @Value("${inventory.max-retry-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${inventory.retry-delay-ms:100}")
    private int retryDelayMs;
    
    /**
     * Atomic reservation with pessimistic locking for critical "last item" scenarios
     * Handles race conditions when multiple users try to buy the last item
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean reserveStock(Long variantId, Integer quantity) {
        return reserveStockWithRetry(variantId, quantity, maxRetryAttempts);
    }
    
    private boolean reserveStockWithRetry(Long variantId, Integer quantity, int maxAttempts) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Use pessimistic lock to prevent concurrent modifications - critical for last item scenarios
                ProductVariant variant = productVariantRepository.findByIdWithLock(variantId)
                        .orElseThrow(() -> new RuntimeException("Product variant not found: " + variantId));
                
                int availableStock = variant.getStockQuantity() - variant.getReservedQuantity();
                
                if (availableStock < quantity) {
                    log.warn("Insufficient stock for variant {} ({}): requested={}, available={}, total={}, reserved={}", 
                            variantId, variant.getSku(), quantity, availableStock, 
                            variant.getStockQuantity(), variant.getReservedQuantity());
                    return false;
                }
                
                // Atomic update - this is where the "last item" race condition is resolved
                variant.setReservedQuantity(variant.getReservedQuantity() + quantity);
                productVariantRepository.save(variant);
                
                log.info("Successfully reserved {} units for variant {} {} (attempt {}). Available stock now: {}", 
                        quantity, variantId, variant.getSku(), attempt, 
                        variant.getStockQuantity() - variant.getReservedQuantity());
                return true;
                
            } catch (OptimisticLockingFailureException e) {
                log.warn("Optimistic locking failure for variant {} (attempt {}): {}", variantId, attempt, e.getMessage());
                if (attempt == maxAttempts) {
                    log.error("Failed to reserve stock for variant {} after {} attempts", variantId, maxAttempts);
                    return false;
                }
                // Exponential backoff to reduce contention
                try {
                    Thread.sleep(retryDelayMs * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            } catch (RuntimeException e) {
                // Re-throw RuntimeExceptions like "Product variant not found" to maintain consistency with other methods
                if (e.getMessage() != null && e.getMessage().contains("Product variant not found")) {
                    throw e;
                }
                log.error("Failed to reserve stock for variant {}: {}", variantId, e.getMessage());
                return false;
            } catch (Exception e) {
                log.error("Failed to reserve stock for variant {}: {}", variantId, e.getMessage());
                return false;
            }
        }
        return false;
    }
    
    /**
     * Batch reservation for multiple items (checkout scenario)
     * CRITICAL: Handles "last item" scenarios with proper locking
     * All-or-nothing approach: either all items are reserved or none
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean reserveMultipleStock(Map<Long, Integer> reservationMap) {
        try {
            log.info("Starting batch reservation for {} variants with SERIALIZABLE isolation", reservationMap.size());
            
            // Step 1: Acquire locks on ALL variants first (prevents deadlocks)
            // This is CRITICAL for "last item" scenarios where multiple users compete
            Map<Long, ProductVariant> lockedVariants = new HashMap<>();
            
            for (Long variantId : reservationMap.keySet()) {
                ProductVariant variant = productVariantRepository.findByIdWithLock(variantId)
                        .orElseThrow(() -> new RuntimeException("Product variant not found: " + variantId));
                lockedVariants.put(variantId, variant);
                
                log.debug("Acquired lock on variant {} ({})", variantId, variant.getSku());
            }
            
            // Step 2: Validate ALL items availability BEFORE making any changes
            // This ensures atomic all-or-nothing behavior
            for (Map.Entry<Long, Integer> entry : reservationMap.entrySet()) {
                Long variantId = entry.getKey();
                Integer requestedQuantity = entry.getValue();
                ProductVariant variant = lockedVariants.get(variantId);
                
                int availableStock = variant.getStockQuantity() - variant.getReservedQuantity();
                
                if (availableStock < requestedQuantity) {
                    log.warn("INSUFFICIENT STOCK - Batch reservation failed for variant {} ({}): " +
                            "requested={}, available={}, total={}, reserved={}", 
                            variantId, variant.getSku(), requestedQuantity, availableStock, 
                            variant.getStockQuantity(), variant.getReservedQuantity());
                    
                    // Return false immediately - no partial reservations
                    return false;
                }
                
                log.debug("Stock validation passed for variant {} ({}): requested={}, available={}", 
                        variantId, variant.getSku(), requestedQuantity, availableStock);
            }
            
            // Step 3: All validations passed - perform ALL reservations atomically
            log.info("All stock validations passed - proceeding with atomic reservations");
            
            for (Map.Entry<Long, Integer> entry : reservationMap.entrySet()) {
                Long variantId = entry.getKey();
                Integer quantity = entry.getValue();
                ProductVariant variant = lockedVariants.get(variantId);
                
                int oldReserved = variant.getReservedQuantity();
                variant.setReservedQuantity(oldReserved + quantity);
                productVariantRepository.save(variant);
                
                log.info("Reserved {} units for variant {} ({}): reserved {} -> {}, available now: {}", 
                        quantity, variantId, variant.getSku(), oldReserved, variant.getReservedQuantity(),
                        variant.getStockQuantity() - variant.getReservedQuantity());
            }
            
            log.info("Successfully completed atomic batch reservation for {} variants", reservationMap.size());
            return true;
            
        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure during batch reservation - likely concurrent access: {}", e.getMessage());
            return false;
            
        } catch (Exception e) {
            log.error("Batch reservation failed with error: {}", e.getMessage(), e);
            throw new RuntimeException("Stock reservation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Release reservation (when checkout expires or order is cancelled)
     * Critical for automatic inventory release after TTL
     */
    @Transactional
    public void releaseReservation(Long variantId, Integer quantity) {
        try {
            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new RuntimeException("Product variant not found: " + variantId));
            
            int currentReserved = variant.getReservedQuantity();
            int newReservedQuantity = Math.max(0, currentReserved - quantity);
            variant.setReservedQuantity(newReservedQuantity);
            productVariantRepository.save(variant);
            
            log.info("Released {} units reservation for variant {} ({}). Reserved: {} -> {}", 
                    quantity, variantId, variant.getSku(), currentReserved, newReservedQuantity);
            
        } catch (Exception e) {
            log.error("Failed to release reservation for variant {}: {}", variantId, e.getMessage());
        }
    }
    
    /**
     * Convert reservation to actual sale (when order is confirmed)
     */
    @Transactional
    public void confirmReservation(Long variantId, Integer quantity) {
        try {
            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new RuntimeException("Product variant not found: " + variantId));
            
            int oldStock = variant.getStockQuantity();
            int oldReserved = variant.getReservedQuantity();
            
            // Decrease both stock and reserved quantities
            variant.setStockQuantity(variant.getStockQuantity() - quantity);
            variant.setReservedQuantity(Math.max(0, variant.getReservedQuantity() - quantity));
            productVariantRepository.save(variant);
            
            log.info("Confirmed reservation for variant {} ({}): sold {} units. Stock: {} -> {}, Reserved: {} -> {}", 
                    variantId, variant.getSku(), quantity, oldStock, variant.getStockQuantity(), 
                    oldReserved, variant.getReservedQuantity());
            
        } catch (Exception e) {
            log.error("Failed to confirm reservation for variant {}: {}", variantId, e.getMessage());
            throw new RuntimeException("Failed to confirm reservation: " + e.getMessage());
        }
    }
    
    /**
     * Get current available stock (considering reservations)
     */
    public int getAvailableStock(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Product variant not found: " + variantId));
        
        return Math.max(0, variant.getStockQuantity() - variant.getReservedQuantity());
    }
    
    /**
     * Batch release reservations (for expired checkout sessions)
     * Used by scheduled task to automatically release expired reservations
     */
    @Transactional
    public void releaseMultipleReservations(Map<Long, Integer> reservationMap) {
        log.info("Releasing batch reservations for {} variants", reservationMap.size());
        
        for (Map.Entry<Long, Integer> entry : reservationMap.entrySet()) {
            releaseReservation(entry.getKey(), entry.getValue());
        }
        
        log.info("Completed batch reservation release");
    }
    
    /**
     * Get detailed stock information for monitoring
     */
    public StockInfo getStockInfo(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Product variant not found: " + variantId));
        
        return new StockInfo(
                variant.getId(),
                variant.getSku(),
                variant.getStockQuantity(),
                variant.getReservedQuantity(),
                variant.getStockQuantity() - variant.getReservedQuantity()
        );
    }
    
    // Inner class for stock information
    public static class StockInfo {
        public final Long variantId;
        public final String sku;
        public final Integer totalStock;
        public final Integer reservedStock;
        public final Integer availableStock;
        
        public StockInfo(Long variantId, String sku, Integer totalStock, Integer reservedStock, Integer availableStock) {
            this.variantId = variantId;
            this.sku = sku;
            this.totalStock = totalStock;
            this.reservedStock = reservedStock;
            this.availableStock = availableStock;
        }
    }
    
    // ========== HIGH-LEVEL RESERVATION OPERATIONS ==========
    
    /**
     * High-level method to reserve inventory for a cart
     * Follows sequence diagram: Client -> InventoryController -> InventoryReservationService
     * Handles: Inventory hold (15 mins fixed), Last item processing, Auto-release on expiry
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public InventoryReservationResponse reserveInventory(InventoryReserveRequest request) {
        final int TTL_MINUTES = 15; // Fixed TTL managed by backend
        log.info("Starting inventory reservation for cart: {} with fixed TTL: {} minutes", 
                request.getCartId(), TTL_MINUTES);
        
        try {
            // Step 1: Validate cart (as per sequence diagram)
            Cart cart = cartRepository.findByCartId(request.getCartId())
                    .orElseThrow(() -> new RuntimeException("Cart not found: " + request.getCartId()));
            
            if (cart.getItems().isEmpty()) {
                throw new RuntimeException("Cart is empty");
            }
            
            // Step 2: BEGIN TRANSACTION (as shown in sequence diagram)
            log.debug("Beginning transaction for cart reservation: {}", request.getCartId());
            
            // Step 3: Prepare reservation map for atomic processing
            Map<Long, Integer> reservationMap = new HashMap<>();
            for (CartItem cartItem : cart.getItems()) {
                Long variantId = cartItem.getProductVariant().getId();
                Integer quantity = cartItem.getQuantity();
                reservationMap.put(variantId, quantity);
                
                log.debug("Preparing to reserve {} units of variant {} ({})", 
                        quantity, variantId, cartItem.getProductVariant().getSku());
            }
            
            // Step 4: Atomic reservation for all items (CRITICAL for last item scenarios)
            log.info("Attempting atomic reservation for {} variants", reservationMap.size());
            boolean reservationSuccess = reserveMultipleStock(reservationMap);
            
            if (!reservationSuccess) {
                log.error("Failed to reserve stock for cart: {}", request.getCartId());
                throw new RuntimeException("Unable to reserve stock - insufficient inventory or last item conflict");
            }
            
            // Step 5: Create checkout session with fixed TTL (15 minutes as per requirement)
            CheckoutSession session = new CheckoutSession();
            session.setCheckoutToken(UUID.randomUUID().toString());
            session.setCartId(request.getCartId());
            session.setExpiresAt(LocalDateTime.now().plusMinutes(TTL_MINUTES));
            session = checkoutSessionRepository.save(session);
            
            log.info("Created checkout session: {} expires at: {}", 
                    session.getCheckoutToken(), session.getExpiresAt());
            
            // Step 6: Create stock reservation records for tracking
            BigDecimal totalAmount = BigDecimal.ZERO;
            int totalItems = 0;
            
            for (CartItem cartItem : cart.getItems()) {
                ProductVariant variant = cartItem.getProductVariant();
                
                // Create reservation record for auto-release mechanism
                StockReservation reservation = new StockReservation();
                reservation.setCheckoutSession(session);
                reservation.setProductVariant(variant);
                reservation.setQuantity(cartItem.getQuantity());
                stockReservationRepository.save(reservation);
                
                totalAmount = totalAmount.add(variant.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                totalItems += cartItem.getQuantity();
                
                log.debug("Created reservation record: {} units of {} for session {}", 
                        cartItem.getQuantity(), variant.getSku(), session.getCheckoutToken());
            }
            
            // Step 7: COMMIT transaction (as per sequence diagram)
            log.info("Successfully reserved inventory for cart: {} with token: {} (Total: ${}, Items: {})", 
                    request.getCartId(), session.getCheckoutToken(), totalAmount, totalItems);
            
            return mapToInventoryReservationResponse(session, cart.getItems(), totalAmount, totalItems);
            
        } catch (Exception e) {
            // Step 8: ROLLBACK on error (as shown in sequence diagram)
            log.error("Error during inventory reservation for cart: {} - Transaction will rollback. Error: {}", 
                    request.getCartId(), e.getMessage(), e);
            
            // Transaction will automatically rollback due to @Transactional
            throw new RuntimeException("Inventory reservation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * High-level method to release a reservation by token
     * Used by: Manual release API + Auto-release scheduled task
     * Ensures proper cleanup when checkout expires (10-15 minutes)
     */
    @Transactional
    public Map<String, Object> releaseReservationByToken(String reservationToken) {
        log.info("Releasing reservation: {}", reservationToken);
        
        try {
            // Step 1: Find checkout session
            CheckoutSession session = checkoutSessionRepository.findByCheckoutToken(reservationToken)
                    .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationToken));
            
            // Step 2: Check if already processed (prevent double-release)
            if (session.getIsUsed()) {
                log.warn("Reservation {} already processed - skipping release", reservationToken);
                Map<String, Object> response = new HashMap<>();
                response.put("reservationToken", reservationToken);
                response.put("status", "ALREADY_PROCESSED");
                response.put("message", "Reservation was already processed");
                response.put("timestamp", LocalDateTime.now());
                return response;
            }
            
            // Step 3: Find all stock reservations for this session
            List<StockReservation> reservations = stockReservationRepository.findByCheckoutSessionId(session.getId());
            
            if (reservations.isEmpty()) {
                log.warn("No stock reservations found for session: {}", reservationToken);
            } else {
                log.info("Found {} stock reservations to release for session: {}", reservations.size(), reservationToken);
            }
            
            // Step 4: Prepare release map
            Map<Long, Integer> reservationMap = reservations.stream()
                    .collect(Collectors.toMap(
                            r -> r.getProductVariant().getId(),
                            StockReservation::getQuantity,
                            Integer::sum  // Handle duplicate variants (shouldn't happen but safe)
                    ));
            
            // Step 5: Release all reservations atomically
            if (!reservationMap.isEmpty()) {
                releaseMultipleReservations(reservationMap);
                log.info("Released reservations for {} variants in session: {}", reservationMap.size(), reservationToken);
            }
            
            // Step 6: Mark session as used to prevent reprocessing
            session.setIsUsed(true);
            checkoutSessionRepository.save(session);
            
            log.info("Successfully released reservation: {} (expired: {}, cart: {})", 
                    reservationToken, session.isExpired(), session.getCartId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("reservationToken", reservationToken);
            response.put("status", "RELEASED");
            response.put("message", "Inventory reservation released successfully");
            response.put("releasedVariants", reservationMap.size());
            response.put("wasExpired", session.isExpired());
            response.put("timestamp", LocalDateTime.now());
            
            return response;
            
        } catch (Exception e) {
            log.error("Failed to release reservation {}: {}", reservationToken, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("reservationToken", reservationToken);
            response.put("status", "ERROR");
            response.put("message", "Failed to release reservation: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            
            throw new RuntimeException("Failed to release reservation: " + e.getMessage(), e);
        }
    }
    
    /**
     * High-level method to confirm a reservation by token
     */
    @Transactional
    public Map<String, Object> confirmReservationByToken(String reservationToken) {
        log.info("Confirming reservation: {}", reservationToken);
        
        CheckoutSession session = checkoutSessionRepository.findByCheckoutToken(reservationToken)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationToken));
        
        List<StockReservation> reservations = stockReservationRepository.findByCheckoutSessionId(session.getId());
        
        for (StockReservation reservation : reservations) {
            // Use existing low-level method
            confirmReservation(
                    reservation.getProductVariant().getId(),
                    reservation.getQuantity()
            );
        }
        
        log.info("Successfully confirmed reservation: {}", reservationToken);
        
        Map<String, Object> response = new HashMap<>();
        response.put("reservationToken", reservationToken);
        response.put("status", "CONFIRMED");
        response.put("message", "Inventory reservation confirmed successfully");
        response.put("timestamp", LocalDateTime.now());
        
        return response;
    }
    
    /**
     * High-level method to get reservation status by token
     */
    public Map<String, Object> getReservationStatusByToken(String reservationToken) {
        CheckoutSession session = checkoutSessionRepository.findByCheckoutToken(reservationToken)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationToken));
        
        Map<String, Object> response = new HashMap<>();
        response.put("reservationToken", reservationToken);
        response.put("cartId", session.getCartId());
        response.put("expiresAt", session.getExpiresAt());
        response.put("isExpired", session.isExpired());
        response.put("isUsed", session.getIsUsed());
        response.put("status", session.isExpired() ? "EXPIRED" : (session.getIsUsed() ? "USED" : "ACTIVE"));
        response.put("timestamp", LocalDateTime.now());
        
        return response;
    }
    
    /**
     * Helper method to map entities to response DTO
     */
    private InventoryReservationResponse mapToInventoryReservationResponse(CheckoutSession session, 
                                                                          List<CartItem> cartItems, 
                                                                          BigDecimal totalAmount, 
                                                                          Integer totalItems) {
        List<InventoryReservationResponse.ReservedItemResponse> items = cartItems.stream()
                .map(item -> {
                    ProductVariant variant = item.getProductVariant();
                    BigDecimal subtotal = variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    
                    return new InventoryReservationResponse.ReservedItemResponse(
                            variant.getId(),
                            variant.getSku(),
                            variant.getProduct().getName(),
                            variant.getSize(),
                            variant.getColor(),
                            variant.getPrice(),
                            item.getQuantity(),
                            subtotal
                    );
                })
                .collect(Collectors.toList());
        
        return new InventoryReservationResponse(
                session.getCheckoutToken(),
                session.getCartId(),
                session.getExpiresAt(),
                items,
                totalAmount,
                totalItems
        );
    }
}