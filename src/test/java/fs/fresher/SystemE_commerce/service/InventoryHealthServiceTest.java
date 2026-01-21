package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.entity.CheckoutSession;
import fs.fresher.SystemE_commerce.repository.CheckoutSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryHealthServiceTest {

    @Mock
    private CheckoutSessionRepository checkoutSessionRepository;

    @InjectMocks
    private InventoryHealthService inventoryHealthService;

    private List<CheckoutSession> mockActiveSessions;
    private List<CheckoutSession> mockExpiredSessions;
    private CheckoutSession activeSession1;
    private CheckoutSession activeSession2;
    private CheckoutSession expiredSession1;
    private CheckoutSession expiredSession2;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        LocalDateTime now = LocalDateTime.now();

        // Setup active sessions
        activeSession1 = new CheckoutSession();
        activeSession1.setId(1L);
        activeSession1.setCheckoutToken("active-token-1");
        activeSession1.setCreatedAt(now.minusMinutes(3)); // 3 minutes old
        activeSession1.setExpiresAt(now.plusMinutes(12)); // Expires in 12 minutes
        activeSession1.setIsUsed(false);

        activeSession2 = new CheckoutSession();
        activeSession2.setId(2L);
        activeSession2.setCheckoutToken("active-token-2");
        activeSession2.setCreatedAt(now.minusMinutes(8)); // 8 minutes old
        activeSession2.setExpiresAt(now.plusMinutes(7)); // Expires in 7 minutes
        activeSession2.setIsUsed(false);

        mockActiveSessions = Arrays.asList(activeSession1, activeSession2);

        // Setup expired sessions
        expiredSession1 = new CheckoutSession();
        expiredSession1.setId(3L);
        expiredSession1.setCheckoutToken("expired-token-1");
        expiredSession1.setCreatedAt(now.minusMinutes(20));
        expiredSession1.setExpiresAt(now.minusMinutes(5)); // Expired 5 minutes ago
        expiredSession1.setIsUsed(false);

        expiredSession2 = new CheckoutSession();
        expiredSession2.setId(4L);
        expiredSession2.setCheckoutToken("expired-token-2");
        expiredSession2.setCreatedAt(now.minusMinutes(25));
        expiredSession2.setExpiresAt(now.minusMinutes(10)); // Expired 10 minutes ago
        expiredSession2.setIsUsed(false);

        mockExpiredSessions = Arrays.asList(expiredSession1, expiredSession2);
    }

    @Test
    void getInventoryHealth_WithNormalLoad_ShouldReturnGoodStatus() {
        // Given
        when(checkoutSessionRepository.findActiveSessions(any(LocalDateTime.class)))
                .thenReturn(mockActiveSessions);
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(mockExpiredSessions);

        // When
        Map<String, Object> result = inventoryHealthService.getInventoryHealth();

        // Then
        assertNotNull(result);
        assertEquals(2, result.get("activeSessions"));
        assertEquals(2, result.get("expiredSessions"));
        assertEquals("GOOD", result.get("status"));
        assertEquals("Inventory system is healthy.", result.get("message"));
        assertNotNull(result.get("timestamp"));

        verify(checkoutSessionRepository).findActiveSessions(any(LocalDateTime.class));
        verify(checkoutSessionRepository).findExpiredSessions(any(LocalDateTime.class));
    }

    @Test
    void getInventoryHealth_WithNoExpiredSessions_ShouldReturnExcellentStatus() {
        // Given
        when(checkoutSessionRepository.findActiveSessions(any(LocalDateTime.class)))
                .thenReturn(mockActiveSessions);
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        // When
        Map<String, Object> result = inventoryHealthService.getInventoryHealth();

        // Then
        assertNotNull(result);
        assertEquals(2, result.get("activeSessions"));
        assertEquals(0, result.get("expiredSessions"));
        assertEquals("EXCELLENT", result.get("status"));
        assertEquals("Inventory system is healthy.", result.get("message"));
    }

    @Test
    void getInventoryHealth_WithHighExpiredSessions_ShouldReturnCriticalStatus() {
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
        Map<String, Object> result = inventoryHealthService.getInventoryHealth();

        // Then
        assertNotNull(result);
        assertEquals(2, result.get("activeSessions"));
        assertEquals(15, result.get("expiredSessions"));
        assertEquals("CRITICAL", result.get("status"));
        assertEquals("High number of expired sessions detected. Check cleanup job performance.", result.get("message"));
        assertEquals("Consider running manual cleanup or check scheduled task configuration.", result.get("recommendation"));
    }

    @Test
    void getInventoryHealth_WithWarningLevel_ShouldReturnWarningStatus() {
        // Given
        List<CheckoutSession> warningLevelExpiredSessions = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            CheckoutSession session = new CheckoutSession();
            session.setId((long) (i + 10));
            session.setCheckoutToken("expired-token-" + i);
            warningLevelExpiredSessions.add(session);
        }

        when(checkoutSessionRepository.findActiveSessions(any(LocalDateTime.class)))
                .thenReturn(mockActiveSessions);
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(warningLevelExpiredSessions);

        // When
        Map<String, Object> result = inventoryHealthService.getInventoryHealth();

        // Then
        assertEquals("WARNING", result.get("status"));
        assertEquals("Inventory system is healthy.", result.get("message"));
    }

    @Test
    void performManualCleanup_WithExpiredSessions_ShouldReturnCleanupResults() {
        // Given
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(mockExpiredSessions);

        // When
        Map<String, Object> result = inventoryHealthService.performManualCleanup();

        // Then
        assertNotNull(result);
        assertEquals(2, result.get("foundExpiredSessions"));
        assertEquals(2, result.get("cleanedSessions"));
        assertTrue(result.get("message").toString().contains("Manual cleanup completed"));
        assertNotNull(result.get("timestamp"));

        verify(checkoutSessionRepository).findExpiredSessions(any(LocalDateTime.class));
    }

    @Test
    void performManualCleanup_WithNoExpiredSessions_ShouldReturnZeroResults() {
        // Given
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        // When
        Map<String, Object> result = inventoryHealthService.performManualCleanup();

        // Then
        assertNotNull(result);
        assertEquals(0, result.get("foundExpiredSessions"));
        assertEquals(0, result.get("cleanedSessions"));
        assertTrue(result.get("message").toString().contains("Processed 0 expired sessions"));
    }

    @Test
    void getDetailedInventoryStats_WithMixedSessions_ShouldReturnDetailedStats() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        // Create sessions with different ages
        CheckoutSession youngSession = new CheckoutSession();
        youngSession.setCreatedAt(now.minusMinutes(2)); // 2 minutes old
        
        CheckoutSession middleSession = new CheckoutSession();
        middleSession.setCreatedAt(now.minusMinutes(10)); // 10 minutes old
        
        CheckoutSession oldSession = new CheckoutSession();
        oldSession.setCreatedAt(now.minusMinutes(20)); // 20 minutes old
        
        List<CheckoutSession> mixedActiveSessions = Arrays.asList(youngSession, middleSession, oldSession);

        when(checkoutSessionRepository.findActiveSessions(any(LocalDateTime.class)))
                .thenReturn(mixedActiveSessions);
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(mockExpiredSessions);
        when(checkoutSessionRepository.count())
                .thenReturn(10L);

        // When
        Map<String, Object> result = inventoryHealthService.getDetailedInventoryStats();

        // Then
        assertNotNull(result);
        assertEquals(10L, result.get("totalSessions"));
        assertEquals(3, result.get("activeSessions"));
        assertEquals(2, result.get("expiredSessions"));
        assertEquals("GOOD", result.get("healthStatus"));

        @SuppressWarnings("unchecked")
        Map<String, Integer> sessionsByAge = (Map<String, Integer>) result.get("sessionsByAge");
        assertNotNull(sessionsByAge);
        assertEquals(1, sessionsByAge.get("lessThan5Minutes")); // youngSession
        assertEquals(1, sessionsByAge.get("5to15Minutes")); // middleSession
        assertEquals(1, sessionsByAge.get("moreThan15Minutes")); // oldSession

        verify(checkoutSessionRepository).findActiveSessions(any(LocalDateTime.class));
        verify(checkoutSessionRepository).findExpiredSessions(any(LocalDateTime.class));
        verify(checkoutSessionRepository).count();
    }

    @Test
    void getDetailedInventoryStats_WithNoActiveSessions_ShouldReturnZeroStats() {
        // Given
        when(checkoutSessionRepository.findActiveSessions(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        when(checkoutSessionRepository.count())
                .thenReturn(0L);

        // When
        Map<String, Object> result = inventoryHealthService.getDetailedInventoryStats();

        // Then
        assertNotNull(result);
        assertEquals(0L, result.get("totalSessions"));
        assertEquals(0, result.get("activeSessions"));
        assertEquals(0, result.get("expiredSessions"));
        assertEquals("EXCELLENT", result.get("healthStatus"));

        @SuppressWarnings("unchecked")
        Map<String, Integer> sessionsByAge = (Map<String, Integer>) result.get("sessionsByAge");
        assertEquals(0, sessionsByAge.get("lessThan5Minutes"));
        assertEquals(0, sessionsByAge.get("5to15Minutes"));
        assertEquals(0, sessionsByAge.get("moreThan15Minutes"));
    }

    @Test
    void getInventoryHealth_WithRepositoryException_ShouldPropagateException() {
        // Given
        when(checkoutSessionRepository.findActiveSessions(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            inventoryHealthService.getInventoryHealth();
        });
    }

    @Test
    void performManualCleanup_WithRepositoryException_ShouldPropagateException() {
        // Given
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            inventoryHealthService.performManualCleanup();
        });
    }

    @Test
    void getDetailedInventoryStats_WithLargeNumberOfSessions_ShouldHandleCorrectly() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<CheckoutSession> largeSessions = new ArrayList<>();
        
        // Create 100 sessions with various ages
        for (int i = 0; i < 100; i++) {
            CheckoutSession session = new CheckoutSession();
            session.setCreatedAt(now.minusMinutes(i % 30)); // Ages from 0 to 29 minutes
            largeSessions.add(session);
        }

        when(checkoutSessionRepository.findActiveSessions(any(LocalDateTime.class)))
                .thenReturn(largeSessions);
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        when(checkoutSessionRepository.count())
                .thenReturn(100L);

        // When
        Map<String, Object> result = inventoryHealthService.getDetailedInventoryStats();

        // Then
        assertNotNull(result);
        assertEquals(100L, result.get("totalSessions"));
        assertEquals(100, result.get("activeSessions"));

        @SuppressWarnings("unchecked")
        Map<String, Integer> sessionsByAge = (Map<String, Integer>) result.get("sessionsByAge");
        
        // Verify that sessions are categorized correctly
        int totalCategorized = sessionsByAge.get("lessThan5Minutes") + 
                              sessionsByAge.get("5to15Minutes") + 
                              sessionsByAge.get("moreThan15Minutes");
        assertEquals(100, totalCategorized);
    }

    @Test
    void determineHealthStatus_BoundaryValues_ShouldReturnCorrectStatus() {
        // Test boundary values through public methods
        
        // Test EXCELLENT (0 expired)
        when(checkoutSessionRepository.findActiveSessions(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        
        Map<String, Object> result = inventoryHealthService.getInventoryHealth();
        assertEquals("EXCELLENT", result.get("status"));

        // Test GOOD (5 expired)
        List<CheckoutSession> fiveExpired = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            fiveExpired.add(new CheckoutSession());
        }
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(fiveExpired);
        
        result = inventoryHealthService.getInventoryHealth();
        assertEquals("GOOD", result.get("status"));

        // Test WARNING (10 expired)
        List<CheckoutSession> tenExpired = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tenExpired.add(new CheckoutSession());
        }
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(tenExpired);
        
        result = inventoryHealthService.getInventoryHealth();
        assertEquals("WARNING", result.get("status"));

        // Test CRITICAL (11 expired)
        List<CheckoutSession> elevenExpired = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            elevenExpired.add(new CheckoutSession());
        }
        when(checkoutSessionRepository.findExpiredSessions(any(LocalDateTime.class)))
                .thenReturn(elevenExpired);
        
        result = inventoryHealthService.getInventoryHealth();
        assertEquals("CRITICAL", result.get("status"));
    }
}