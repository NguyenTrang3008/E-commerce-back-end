package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.entity.CheckoutSession;
import fs.fresher.SystemE_commerce.repository.CheckoutSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {
    
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final InventoryReservationService inventoryReservationService;
    private final CartCleanupService cartCleanupService;
    
    @Value("${inventory.cleanup-interval-seconds:60}")
    private int cleanupIntervalSeconds;
    
    @Value("${inventory.old-session-cleanup-hours:1}")
    private int oldSessionCleanupHours;
    
    @Value("${cart.cleanup-interval-hours:6}")
    private int cartCleanupIntervalHours;
    
    /**
     * CRITICAL: Auto-release expired inventory reservations (10-15 minutes TTL)
     * Runs every minute to ensure timely release of "last item" scenarios
     * Prevents inventory from being locked up by abandoned checkout sessions
     */
    @Scheduled(fixedRateString = "${inventory.cleanup-interval-seconds:60}000")
    @Transactional
    public void cleanupExpiredCheckoutSessions() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<CheckoutSession> expiredSessions = checkoutSessionRepository.findExpiredSessions(now);
            
            if (expiredSessions.isEmpty()) {
                log.debug("No expired checkout sessions found at {}", now);
                return;
            }
            
            log.info("AUTO-RELEASE: Found {} expired checkout sessions to process at {}", 
                    expiredSessions.size(), now);
            
            int processedCount = 0;
            int errorCount = 0;
            int alreadyProcessedCount = 0;
            
            for (CheckoutSession session : expiredSessions) {
                if (!session.getIsUsed()) {
                    try {
                        log.info("AUTO-RELEASE: Processing expired session: {} (expired at: {}, cart: {})", 
                                session.getCheckoutToken(), session.getExpiresAt(), session.getCartId());
                        
                        // Use InventoryReservationService for proper release handling
                        var result = inventoryReservationService.releaseReservationByToken(session.getCheckoutToken());
                        
                        processedCount++;
                        log.info("AUTO-RELEASE: Successfully released reservations for expired session: {} (released {} variants)", 
                                session.getCheckoutToken(), result.get("releasedVariants"));
                        
                    } catch (Exception e) {
                        errorCount++;
                        log.error("AUTO-RELEASE: Failed to release reservations for session {}: {}", 
                                session.getCheckoutToken(), e.getMessage(), e);
                    }
                } else {
                    alreadyProcessedCount++;
                    log.debug("AUTO-RELEASE: Session {} already processed", session.getCheckoutToken());
                }
            }
            
            log.info("AUTO-RELEASE: Cleanup completed - {} processed, {} already processed, {} errors out of {} total", 
                    processedCount, alreadyProcessedCount, errorCount, expiredSessions.size());
            
            // Alert if high error rate
            if (errorCount > 0 && errorCount > processedCount / 2) {
                log.error("AUTO-RELEASE: HIGH ERROR RATE detected - {} errors out of {} attempts. Check system health!", 
                        errorCount, processedCount + errorCount);
            }
            
        } catch (Exception e) {
            log.error("AUTO-RELEASE: CRITICAL ERROR in expired checkout sessions cleanup job", e);
        }
    }
    
    /**
     * Housekeeping task - removes old processed sessions to keep database clean
     * Runs less frequently as it's not critical for inventory management
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void cleanupOldCheckoutSessions() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(oldSessionCleanupHours);
            List<CheckoutSession> oldSessions = checkoutSessionRepository.findExpiredSessions(cutoffTime);
            
            int deletedCount = 0;
            for (CheckoutSession session : oldSessions) {
                if (session.getIsUsed()) {
                    try {
                        checkoutSessionRepository.delete(session);
                        deletedCount++;
                        log.debug("Deleted old processed session: {}", session.getCheckoutToken());
                    } catch (Exception e) {
                        log.warn("Failed to delete old session {}: {}", session.getCheckoutToken(), e.getMessage());
                    }
                }
            }
            
            if (deletedCount > 0) {
                log.info("Cleaned up {} old checkout sessions (older than {} hours)", deletedCount, oldSessionCleanupHours);
            }
            
        } catch (Exception e) {
            log.error("Error in old checkout sessions cleanup job", e);
        }
    }
    
    /**
     * Health check task - monitors inventory reservation health
     * Logs statistics about current reservations for monitoring
     */
    @Scheduled(fixedRate = 900000) // Every 15 minutes
    public void logInventoryHealth() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Count active sessions
            List<CheckoutSession> activeSessions = checkoutSessionRepository.findActiveSessions(now);
            List<CheckoutSession> expiredSessions = checkoutSessionRepository.findExpiredSessions(now);
            
            log.info("Inventory Health Check - Active sessions: {}, Expired sessions: {}, Time: {}", 
                    activeSessions.size(), expiredSessions.size(), now);
            
            // Log warning if too many expired sessions (might indicate cleanup issues)
            if (expiredSessions.size() > 10) {
                log.warn("High number of expired sessions detected: {}. Check cleanup job performance.", 
                        expiredSessions.size());
            }
            
        } catch (Exception e) {
            log.error("Error in inventory health check", e);
        }
    }
    
    /**
     * CART CLEANUP: Clean up expired and inactive carts
     * Runs every 6 hours by default to maintain database hygiene
     */
    @Scheduled(fixedRateString = "${cart.cleanup-interval-hours:6}000000") // Convert hours to milliseconds
    @Transactional
    public void cleanupExpiredCarts() {
        try {
            log.info("CART CLEANUP: Starting scheduled cart cleanup");
            
            // Clean up expired carts
            var expiredResult = cartCleanupService.cleanupExpiredCarts();
            
            // Clean up empty inactive carts
            var emptyResult = cartCleanupService.cleanupEmptyInactiveCarts();
            
            // Get statistics for monitoring
            var stats = cartCleanupService.getCartStatistics();
            
            log.info("CART CLEANUP: Completed - Expired: {} deleted, Empty inactive: {} deleted, Total remaining: {}", 
                    expiredResult.get("deletedCarts"), 
                    emptyResult.get("deletedCarts"),
                    stats.get("totalCarts"));
            
        } catch (Exception e) {
            log.error("CART CLEANUP: CRITICAL ERROR in cart cleanup job", e);
        }
    }
    
    /**
     * CART HEALTH: Monitor cart health and log statistics
     * Runs every 30 minutes for monitoring purposes
     */
    @Scheduled(fixedRate = 1800000) // Every 30 minutes
    public void logCartHealth() {
        try {
            var stats = cartCleanupService.getCartStatistics();
            
            log.info("Cart Health Check - Total: {}, Expired: {}, Inactive: {}, Empty: {}", 
                    stats.get("totalCarts"),
                    stats.get("expiredCarts"), 
                    stats.get("inactiveCarts"),
                    stats.get("emptyCarts"));
            
            // Log warning if too many expired carts (might indicate cleanup issues)
            int expiredCount = (Integer) stats.get("expiredCarts");
            if (expiredCount > 50) {
                log.warn("High number of expired carts detected: {}. Check cart cleanup job performance.", 
                        expiredCount);
            }
            
        } catch (Exception e) {
            log.error("Error in cart health check", e);
        }
    }
}