package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.response.OrderTrackingResponse;
import fs.fresher.SystemE_commerce.exception.ValidationException;
import fs.fresher.SystemE_commerce.validator.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {
    
    private final OrderService orderService;
    private final ValidationService validationService;
    
    /**
     * Get tracking data for API
     */
    public OrderTrackingResponse getTrackingData(String trackingToken) {
        validationService.validateTrackingToken(trackingToken);
        return orderService.trackOrder(trackingToken);
    }
}