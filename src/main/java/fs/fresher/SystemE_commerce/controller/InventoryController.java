package fs.fresher.SystemE_commerce.controller;

import fs.fresher.SystemE_commerce.dto.request.InventoryReserveRequest;
import fs.fresher.SystemE_commerce.dto.response.InventoryReservationResponse;
import fs.fresher.SystemE_commerce.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    
    private final InventoryReservationService inventoryReservationService;
    
    @PostMapping("/reserve")
    public ResponseEntity<InventoryReservationResponse> reserveInventory(@Valid @RequestBody InventoryReserveRequest request) {
        InventoryReservationResponse response = inventoryReservationService.reserveInventory(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/release/{reservationToken}")
    public ResponseEntity<Map<String, Object>> releaseReservation(@PathVariable String reservationToken) {
        Map<String, Object> response = inventoryReservationService.releaseReservationByToken(reservationToken);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/confirm/{reservationToken}")
    public ResponseEntity<Map<String, Object>> confirmReservation(@PathVariable String reservationToken) {
        Map<String, Object> response = inventoryReservationService.confirmReservationByToken(reservationToken);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/reservation/{reservationToken}")
    public ResponseEntity<Map<String, Object>> getReservationStatus(@PathVariable String reservationToken) {
        Map<String, Object> response = inventoryReservationService.getReservationStatusByToken(reservationToken);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/stock/{variantId}")
    public ResponseEntity<InventoryReservationService.StockInfo> getStockInfo(@PathVariable Long variantId) {
        InventoryReservationService.StockInfo stockInfo = inventoryReservationService.getStockInfo(variantId);
        return ResponseEntity.ok(stockInfo);
    }
    
    @GetMapping("/available/{variantId}")
    public ResponseEntity<Map<String, Object>> getAvailableStock(@PathVariable Long variantId) {
        int availableStock = inventoryReservationService.getAvailableStock(variantId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("variantId", variantId);
        response.put("availableStock", availableStock);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}