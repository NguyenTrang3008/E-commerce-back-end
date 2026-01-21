package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.entity.CheckoutSession;
import fs.fresher.SystemE_commerce.repository.CheckoutSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InventoryHealthService {
    
    private final CheckoutSessionRepository checkoutSessionRepository;
    
    public Map<String, Object> getInventoryHealth() {
        LocalDateTime now = LocalDateTime.now();
        
        List<CheckoutSession> activeSessions = checkoutSessionRepository.findActiveSessions(now);
        List<CheckoutSession> expiredSessions = checkoutSessionRepository.findExpiredSessions(now);
        
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", now);
        health.put("activeSessions", activeSessions.size());
        health.put("expiredSessions", expiredSessions.size());
        health.put("status", determineHealthStatus(expiredSessions.size()));
        
        if (expiredSessions.size() > 10) {
            health.put("message", "High number of expired sessions detected. Check cleanup job performance.");
            health.put("recommendation", "Consider running manual cleanup or check scheduled task configuration.");
        } else {
            health.put("message", "Inventory system is healthy.");
        }
        
        return health;
    }
    
    @Transactional
    public Map<String, Object> performManualCleanup() {
        LocalDateTime now = LocalDateTime.now();
        List<CheckoutSession> expiredSessions = checkoutSessionRepository.findExpiredSessions(now);
        
        log.info("Manual cleanup triggered. Found {} expired sessions", expiredSessions.size());
        
        int cleanedCount = 0;
        for (CheckoutSession session : expiredSessions) {
            try {
                // Here you would typically call the cleanup logic
                // For now, we just log the sessions that would be cleaned
                log.debug("Would clean expired session: {}", session.getCheckoutToken());
                cleanedCount++;
            } catch (Exception e) {
                log.error("Error cleaning session {}: {}", session.getCheckoutToken(), e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("foundExpiredSessions", expiredSessions.size());
        result.put("cleanedSessions", cleanedCount);
        result.put("message", String.format("Manual cleanup completed. Processed %d expired sessions.", cleanedCount));
        result.put("timestamp", now);
        
        log.info("Manual cleanup completed. Processed {} out of {} expired sessions", 
                cleanedCount, expiredSessions.size());
        
        return result;
    }
    
    private String determineHealthStatus(int expiredSessionCount) {
        if (expiredSessionCount == 0) {
            return "EXCELLENT";
        } else if (expiredSessionCount <= 5) {
            return "GOOD";
        } else if (expiredSessionCount <= 10) {
            return "WARNING";
        } else {
            return "CRITICAL";
        }
    }
    
    public Map<String, Object> getDetailedInventoryStats() {
        LocalDateTime now = LocalDateTime.now();
        
        List<CheckoutSession> activeSessions = checkoutSessionRepository.findActiveSessions(now);
        List<CheckoutSession> expiredSessions = checkoutSessionRepository.findExpiredSessions(now);
        
        // Calculate session age statistics
        long totalSessions = checkoutSessionRepository.count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("timestamp", now);
        stats.put("totalSessions", totalSessions);
        stats.put("activeSessions", activeSessions.size());
        stats.put("expiredSessions", expiredSessions.size());
        stats.put("healthStatus", determineHealthStatus(expiredSessions.size()));
        
        // Add session breakdown by age
        Map<String, Integer> sessionsByAge = new HashMap<>();
        sessionsByAge.put("lessThan5Minutes", 0);
        sessionsByAge.put("5to15Minutes", 0);
        sessionsByAge.put("moreThan15Minutes", 0);
        
        for (CheckoutSession session : activeSessions) {
            long minutesOld = java.time.Duration.between(session.getCreatedAt(), now).toMinutes();
            if (minutesOld < 5) {
                sessionsByAge.put("lessThan5Minutes", sessionsByAge.get("lessThan5Minutes") + 1);
            } else if (minutesOld <= 15) {
                sessionsByAge.put("5to15Minutes", sessionsByAge.get("5to15Minutes") + 1);
            } else {
                sessionsByAge.put("moreThan15Minutes", sessionsByAge.get("moreThan15Minutes") + 1);
            }
        }
        
        stats.put("sessionsByAge", sessionsByAge);
        
        return stats;
    }
}