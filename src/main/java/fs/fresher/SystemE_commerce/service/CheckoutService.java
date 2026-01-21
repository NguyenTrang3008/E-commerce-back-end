package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.request.CheckoutStartRequest;
import fs.fresher.SystemE_commerce.dto.request.InventoryReserveRequest;
import fs.fresher.SystemE_commerce.dto.response.CheckoutSessionResponse;
import fs.fresher.SystemE_commerce.dto.response.InventoryReservationResponse;
import fs.fresher.SystemE_commerce.entity.*;
import fs.fresher.SystemE_commerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
@Transactional
public class CheckoutService {
    
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final StockReservationRepository stockReservationRepository;
    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryReservationService inventoryReservationService;
    
    public CheckoutSessionResponse startCheckout(CheckoutStartRequest request) {
        // Delegate to InventoryReservationService for consistency
        InventoryReserveRequest inventoryRequest = new InventoryReserveRequest();
        inventoryRequest.setCartId(request.getCartId());
        // TTL is now fixed at 15 minutes and managed by backend
        
        InventoryReservationResponse inventoryResponse = inventoryReservationService.reserveInventory(inventoryRequest);
        
        // Map to CheckoutSessionResponse for backward compatibility
        List<CheckoutSessionResponse.ReservedItemResponse> items = inventoryResponse.getReservedItems().stream()
                .map(item -> new CheckoutSessionResponse.ReservedItemResponse(
                        item.getVariantId(),
                        item.getSku(),
                        item.getProductName(),
                        item.getSize(),
                        item.getColor(),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                ))
                .collect(Collectors.toList());
        
        return new CheckoutSessionResponse(
                inventoryResponse.getReservationToken(),
                inventoryResponse.getCartId(),
                inventoryResponse.getExpiresAt(),
                items,
                inventoryResponse.getTotalAmount(),
                inventoryResponse.getTotalItems()
        );
    }
    
    public CheckoutSession validateCheckoutSession(String checkoutToken) {
        CheckoutSession session = checkoutSessionRepository.findByCheckoutToken(checkoutToken)
                .orElseThrow(() -> new RuntimeException("Invalid checkout token"));
        
        if (session.getIsUsed()) {
            throw new RuntimeException("Checkout session already used");
        }
        
        if (session.isExpired()) {
            // Auto-release expired reservations
            releaseReservations(checkoutToken);
            throw new RuntimeException("Checkout session expired");
        }
        
        return session;
    }
    
    public void markSessionAsUsed(String checkoutToken) {
        CheckoutSession session = checkoutSessionRepository.findByCheckoutToken(checkoutToken)
                .orElseThrow(() -> new RuntimeException("Checkout session not found"));
        
        session.setIsUsed(true);
        checkoutSessionRepository.save(session);
    }
    
    public void releaseReservations(String checkoutToken) {
        inventoryReservationService.releaseReservationByToken(checkoutToken);
    }
    
    public void confirmReservations(String checkoutToken) {
        inventoryReservationService.confirmReservationByToken(checkoutToken);
    }
}