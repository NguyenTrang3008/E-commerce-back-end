package fs.fresher.SystemE_commerce.controller;

import fs.fresher.SystemE_commerce.dto.response.OrderTrackingResponse;
import fs.fresher.SystemE_commerce.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TrackingController {
    
    private final TrackingService trackingService;
    
    /**
     * Public tracking endpoint - no login required (for email links)
     * URL: /track/{trackingToken}
     * Returns: JSON response
     */
    @GetMapping("/track/{trackingToken}")
    public ResponseEntity<OrderTrackingResponse> trackOrderPage(@PathVariable String trackingToken) {
        OrderTrackingResponse response = trackingService.getTrackingData(trackingToken);
        return ResponseEntity.ok(response);
    }
    
    /**
     * API endpoint for programmatic access (AJAX, mobile apps, etc.)
     * URL: /api/tracking/{trackingToken}
     * Returns: JSON response
     */
    @GetMapping("/api/tracking/{trackingToken}")
    public ResponseEntity<OrderTrackingResponse> trackOrderApi(@PathVariable String trackingToken) {
        OrderTrackingResponse response = trackingService.getTrackingData(trackingToken);
        return ResponseEntity.ok(response);
    }
}