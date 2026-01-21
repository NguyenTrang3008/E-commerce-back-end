package fs.fresher.SystemE_commerce.controller;

import fs.fresher.SystemE_commerce.service.InventoryHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory/health")
@RequiredArgsConstructor
public class InventoryHealthController {
    
    private final InventoryHealthService inventoryHealthService;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getInventoryHealth() {
        Map<String, Object> health = inventoryHealthService.getInventoryHealth();
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedInventoryStats() {
        Map<String, Object> stats = inventoryHealthService.getDetailedInventoryStats();
        return ResponseEntity.ok(stats);
    }
    
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> performManualCleanup() {
        Map<String, Object> result = inventoryHealthService.performManualCleanup();
        return ResponseEntity.ok(result);
    }
}