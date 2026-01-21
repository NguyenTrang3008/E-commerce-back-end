package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.entity.CheckoutSession;
import fs.fresher.SystemE_commerce.repository.CheckoutSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledTaskServiceTest {

    @Mock
    private CheckoutSessionRepository checkoutSessionRepository;

    @Mock
    private InventoryReservationService inventoryReservationService;
    
    @Mock
    private CartCleanupService cartCleanupService;

    @InjectMocks
    private ScheduledTaskService scheduledTaskService;

    private List<CheckoutSession> mockExpiredSessions;
    private List<CheckoutSession> mockActiveSessions;
    private CheckoutSession expiredSession1;
    private CheckoutSession expiredSession2;
    private CheckoutSession processedSession;

    @BeforeEach
    void setUp() {
        // Set up configuration
        ReflectionTestUtils.setField(scheduledTaskService, "cleanupIntervalSeconds", 60);
        ReflectionTestUtils.setField(scheduledTaskService, "oldSessionCleanupHours", 1);
        ReflectionTestUtils.setField(scheduledTaskService, "cartCleanupIntervalHours", 6);
        
        setupTestData();
    }

    private void setupTestData() {
        LocalDateTime now = LocalDateTime.now();

        // Setup expired sessions (not yet processed)
        expiredSession1 = new CheckoutSession();
        expiredSession1.setId(1L);
        expiredSession1.setCheckoutToken("expired-token-1");
        expiredSession1.setCartId("cart-001");
        expiredSession1.setExpiresAt(now.minusMinutes(5));
        expiredSession1.setIsUsed(false);

        expiredSession2 = new CheckoutSession();
        expiredSession2.setId(2L);
        expiredSession2.setCheckoutToken("expired-token-2");
        expiredSession2.setCartId("cart-002");
        expiredSession2.setExpiresAt(now.minusMinutes(10));
        expiredSession2.setIsUsed(false);

        // Setup processed session (already used)
        processedSession = new CheckoutSession();
        processedSession.setId(3L);
        processedSession.setCheckoutToken("processed-token-1");
        processedSession.setCartId("cart-003");
        processedSession.setExpiresAt(now.minusMinutes(15));
        processedSession.setIsUsed(true);

        mockExpiredSessions = Arrays.asList(expiredSession1, expiredSession2, processedSession);

        // Setup active sessions
        CheckoutSession activeSession = new CheckoutSession();
        activeSession.setId(4L);
        activeSession.setCheckoutToken("active-token-1");
        activeSession.setCartId("cart-004");
        activeSession.setExpiresAt(now.plusMinutes(10));
        activeSession.setIsUsed(false);

        mockActiveSessions = Arrays.asList(activeSession);
    }

    @Test
    void cleanupExpiredCheckoutSessions_WithExpiredSessions_ShouldProcessThem() {
        // Given
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(mockExpiredSessions);

        Map<String, Object> releaseResult = new HashMap<>();
        releaseResult.put("releasedVariants", 2);
        releaseResult.put("status", "RELEASED");

        when(inventoryReservationService.releaseReservationByToken("expired-token-1"))
                .thenReturn(releaseResult);
        when(inventoryReservationService.releaseReservationByToken("expired-token-2"))
                .thenReturn(releaseResult);

        // When
        scheduledTaskService.cleanupExpiredCheckoutSessions();

        // Then
        verify(checkoutSessionRepository).findExpiredSessions(any(LocalDateTime.class));
        verify(inventoryReservationService).releaseReservationByToken("expired-token-1");
        verify(inventoryReservationService).releaseReservationByToken("expired-token-2");
        // Should not process already used session
        verify(inventoryReservationService, never()).releaseReservationByToken("processed-token-1");
    }

    @Test
    void cleanupExpiredCheckoutSessions_WithNoExpiredSessions_ShouldDoNothing() {
        // Given
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        // When
        scheduledTaskService.cleanupExpiredCheckoutSessions();

        // Then
        verify(checkoutSessionRepository).findExpiredSessions(any(LocalDateTime.class));
        verify(inventoryReservationService, never()).releaseReservationByToken(anyString());
    }

    @Test
    void cleanupExpiredCheckoutSessions_WithReleaseException_ShouldContinueProcessing() {
        // Given
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(expiredSession1, expiredSession2));

        Map<String, Object> releaseResult = new HashMap<>();
        releaseResult.put("releasedVariants", 2);

        when(inventoryReservationService.releaseReservationByToken("expired-token-1"))
                .thenThrow(new RuntimeException("Release failed"));
        when(inventoryReservationService.releaseReservationByToken("expired-token-2"))
                .thenReturn(releaseResult);

        // When
        scheduledTaskService.cleanupExpiredCheckoutSessions();

        // Then
        verify(inventoryReservationService).releaseReservationByToken("expired-token-1");
        verify(inventoryReservationService).releaseReservationByToken("expired-token-2");
        // Should continue processing despite first failure
    }

    @Test
    void cleanupExpiredCheckoutSessions_WithRepositoryException_ShouldHandleGracefully() {
        // Given
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            scheduledTaskService.cleanupExpiredCheckoutSessions();
        });

        verify(inventoryReservationService, never()).releaseReservationByToken(anyString());
    }

    @Test
    void cleanupOldCheckoutSessions_WithOldProcessedSessions_ShouldDeleteThem() {
        // Given
        List<CheckoutSession> oldProcessedSessions = Arrays.asList(processedSession);
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(oldProcessedSessions);
        doNothing().when(checkoutSessionRepository).delete(any(CheckoutSession.class));

        // When
        scheduledTaskService.cleanupOldCheckoutSessions();

        // Then
        verify(checkoutSessionRepository).findExpiredSessions(any(LocalDateTime.class));
        verify(checkoutSessionRepository).delete(processedSession);
    }

    @Test
    void cleanupOldCheckoutSessions_WithUnprocessedSessions_ShouldNotDeleteThem() {
        // Given
        List<CheckoutSession> unprocessedSessions = Arrays.asList(expiredSession1);
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(unprocessedSessions);

        // When
        scheduledTaskService.cleanupOldCheckoutSessions();

        // Then
        verify(checkoutSessionRepository).findExpiredSessions(any(LocalDateTime.class));
        verify(checkoutSessionRepository, never()).delete(any(CheckoutSession.class));
    }

    @Test
    void cleanupOldCheckoutSessions_WithDeleteException_ShouldContinueProcessing() {
        // Given
        CheckoutSession processedSession2 = new CheckoutSession();
        processedSession2.setId(5L);
        processedSession2.setCheckoutToken("processed-token-2");
        processedSession2.setIsUsed(true);

        List<CheckoutSession> oldProcessedSessions = Arrays.asList(processedSession, processedSession2);
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(oldProcessedSessions);
        
        doThrow(new RuntimeException("Delete failed")).when(checkoutSessionRepository).delete(processedSession);
        doNothing().when(checkoutSessionRepository).delete(processedSession2);

        // When
        scheduledTaskService.cleanupOldCheckoutSessions();

        // Then
        verify(checkoutSessionRepository).delete(processedSession);
        verify(checkoutSessionRepository).delete(processedSession2);
        // Should continue processing despite first failure
    }

    @Test
    void cleanupOldCheckoutSessions_WithNoOldSessions_ShouldDoNothing() {
        // Given
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        // When
        scheduledTaskService.cleanupOldCheckoutSessions();

        // Then
        verify(checkoutSessionRepository).findExpiredSessions(any(LocalDateTime.class));
        verify(checkoutSessionRepository, never()).delete(any(CheckoutSession.class));
    }

    @Test
    void cleanupOldCheckoutSessions_WithRepositoryException_ShouldHandleGracefully() {
        // Given
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            scheduledTaskService.cleanupOldCheckoutSessions();
        });

        verify(checkoutSessionRepository, never()).delete(any(CheckoutSession.class));
    }

    @Test
    void logInventoryHealth_WithNormalLoad_ShouldLogHealthInfo() {
        // Given
        when(checkoutSessionRepository.findActiveSessions(any(LocalDateTime.class)))
                .thenReturn(mockActiveSessions);
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(expiredSession1, expiredSession2)); // 2 expired sessions

        // When
        scheduledTaskService.logInventoryHealth();

        // Then
        verify(checkoutSessionRepository).findActiveSessions(any(LocalDateTime.class));
        verify(checkoutSessionRepository).findExpiredSessions(any(LocalDateTime.class));
        // Should log health info (we can't easily test log output in unit tests)
    }

    @Test
    void logInventoryHealth_WithHighExpiredSessions_ShouldLogWarning() {
        // Given
        List<CheckoutSession> manyExpiredSessions = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            CheckoutSession session = new CheckoutSession();
            session.setId((long) (i + 10));
            session.setCheckoutToken("expired-token-" + i);
            manyExpiredSessions.add(session);
        }

        when(checkoutSessionRepository.findActiveSessions(any(LocalDateTime.class)))
                .thenReturn(mockActiveSessions);
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(manyExpiredSessions);

        // When
        scheduledTaskService.logInventoryHealth();

        // Then
        verify(checkoutSessionRepository).findActiveSessions(any(LocalDateTime.class));
        verify(checkoutSessionRepository).findExpiredSessions(any(LocalDateTime.class));
        // Should log warning about high number of expired sessions
    }

    @Test
    void logInventoryHealth_WithRepositoryException_ShouldHandleGracefully() {
        // Given
        when(checkoutSessionRepository.findActiveSessions(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            scheduledTaskService.logInventoryHealth();
        });
    }

    @Test
    void cleanupExpiredCheckoutSessions_WithMixedProcessedAndUnprocessedSessions_ShouldProcessCorrectly() {
        // Given
        List<CheckoutSession> mixedSessions = Arrays.asList(
                expiredSession1,    // Not processed - should be released
                processedSession,   // Already processed - should be skipped
                expiredSession2     // Not processed - should be released
        );

        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(mixedSessions);

        Map<String, Object> releaseResult = new HashMap<>();
        releaseResult.put("releasedVariants", 1);

        when(inventoryReservationService.releaseReservationByToken("expired-token-1"))
                .thenReturn(releaseResult);
        when(inventoryReservationService.releaseReservationByToken("expired-token-2"))
                .thenReturn(releaseResult);

        // When
        scheduledTaskService.cleanupExpiredCheckoutSessions();

        // Then
        verify(inventoryReservationService).releaseReservationByToken("expired-token-1");
        verify(inventoryReservationService).releaseReservationByToken("expired-token-2");
        verify(inventoryReservationService, never()).releaseReservationByToken("processed-token-1");
    }

    @Test
    void cleanupExpiredCheckoutSessions_WithAllSessionsAlreadyProcessed_ShouldSkipAll() {
        // Given
        List<CheckoutSession> allProcessedSessions = Arrays.asList(processedSession);
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(allProcessedSessions);

        // When
        scheduledTaskService.cleanupExpiredCheckoutSessions();

        // Then
        verify(checkoutSessionRepository).findExpiredSessions(any(LocalDateTime.class));
        verify(inventoryReservationService, never()).releaseReservationByToken(anyString());
    }

    @Test
    void logInventoryHealth_WithNoActiveSessions_ShouldLogZeroCount() {
        // Given
        when(checkoutSessionRepository.findActiveSessions(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        // When
        scheduledTaskService.logInventoryHealth();

        // Then
        verify(checkoutSessionRepository).findActiveSessions(any(LocalDateTime.class));
        verify(checkoutSessionRepository).findExpiredSessions(any(LocalDateTime.class));
        // Should log zero counts for both active and expired sessions
    }

    @Test
    void cleanupOldCheckoutSessions_WithMixedProcessedAndUnprocessedOldSessions_ShouldDeleteOnlyProcessed() {
        // Given
        CheckoutSession oldUnprocessedSession = new CheckoutSession();
        oldUnprocessedSession.setId(6L);
        oldUnprocessedSession.setCheckoutToken("old-unprocessed-token");
        oldUnprocessedSession.setIsUsed(false);

        List<CheckoutSession> mixedOldSessions = Arrays.asList(processedSession, oldUnprocessedSession);
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(mixedOldSessions);
        doNothing().when(checkoutSessionRepository).delete(any(CheckoutSession.class));

        // When
        scheduledTaskService.cleanupOldCheckoutSessions();

        // Then
        verify(checkoutSessionRepository).delete(processedSession);
        verify(checkoutSessionRepository, never()).delete(oldUnprocessedSession);
    }
}