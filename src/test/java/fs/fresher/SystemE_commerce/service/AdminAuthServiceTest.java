package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.entity.AdminUser;
import fs.fresher.SystemE_commerce.repository.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AdminAuthService adminAuthService;

    private AdminUser mockAdminUser;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        mockAdminUser = new AdminUser();
        mockAdminUser.setId(1L);
        mockAdminUser.setUsername("admin");
        mockAdminUser.setApiKey("valid-api-key-123");
        mockAdminUser.setIsActive(true);
        mockAdminUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void authenticateByApiKey_WithValidApiKey_ShouldReturnAdminUser() {
        // Given
        when(adminUserRepository.findByApiKeyAndIsActiveTrue("valid-api-key-123"))
                .thenReturn(Optional.of(mockAdminUser));

        // When
        Optional<AdminUser> result = adminAuthService.authenticateByApiKey("valid-api-key-123");

        // Then
        assertTrue(result.isPresent());
        assertEquals(mockAdminUser.getId(), result.get().getId());
        assertEquals(mockAdminUser.getUsername(), result.get().getUsername());
        assertEquals(mockAdminUser.getApiKey(), result.get().getApiKey());
        
        verify(adminUserRepository).findByApiKeyAndIsActiveTrue("valid-api-key-123");
    }

    @Test
    void authenticateByApiKey_WithInvalidApiKey_ShouldReturnEmpty() {
        // Given
        when(adminUserRepository.findByApiKeyAndIsActiveTrue("invalid-api-key"))
                .thenReturn(Optional.empty());

        // When
        Optional<AdminUser> result = adminAuthService.authenticateByApiKey("invalid-api-key");

        // Then
        assertFalse(result.isPresent());
        verify(adminUserRepository).findByApiKeyAndIsActiveTrue("invalid-api-key");
    }

    @Test
    void authenticateByApiKey_WithNullApiKey_ShouldReturnEmpty() {
        // When
        Optional<AdminUser> result = adminAuthService.authenticateByApiKey(null);

        // Then
        assertFalse(result.isPresent());
        verify(adminUserRepository, never()).findByApiKeyAndIsActiveTrue(anyString());
    }

    @Test
    void authenticateByApiKey_WithEmptyApiKey_ShouldReturnEmpty() {
        // When
        Optional<AdminUser> result = adminAuthService.authenticateByApiKey("");

        // Then
        assertFalse(result.isPresent());
        verify(adminUserRepository, never()).findByApiKeyAndIsActiveTrue(anyString());
    }

    @Test
    void authenticateByApiKey_WithWhitespaceApiKey_ShouldReturnEmpty() {
        // When
        Optional<AdminUser> result = adminAuthService.authenticateByApiKey("   ");

        // Then
        assertFalse(result.isPresent());
        verify(adminUserRepository, never()).findByApiKeyAndIsActiveTrue(anyString());
    }

    @Test
    void authenticateByUsername_WithValidUsername_ShouldReturnAdminUser() {
        // Given
        when(adminUserRepository.findByUsernameAndIsActiveTrue("admin"))
                .thenReturn(Optional.of(mockAdminUser));

        // When
        Optional<AdminUser> result = adminAuthService.authenticateByUsername("admin");

        // Then
        assertTrue(result.isPresent());
        assertEquals(mockAdminUser.getId(), result.get().getId());
        assertEquals(mockAdminUser.getUsername(), result.get().getUsername());
        
        verify(adminUserRepository).findByUsernameAndIsActiveTrue("admin");
    }

    @Test
    void authenticateByUsername_WithInvalidUsername_ShouldReturnEmpty() {
        // Given
        when(adminUserRepository.findByUsernameAndIsActiveTrue("invalid-user"))
                .thenReturn(Optional.empty());

        // When
        Optional<AdminUser> result = adminAuthService.authenticateByUsername("invalid-user");

        // Then
        assertFalse(result.isPresent());
        verify(adminUserRepository).findByUsernameAndIsActiveTrue("invalid-user");
    }

    @Test
    void authenticateByUsername_WithNullUsername_ShouldReturnEmpty() {
        // When
        Optional<AdminUser> result = adminAuthService.authenticateByUsername(null);

        // Then
        assertFalse(result.isPresent());
        verify(adminUserRepository, never()).findByUsernameAndIsActiveTrue(anyString());
    }

    @Test
    void authenticateByUsername_WithEmptyUsername_ShouldReturnEmpty() {
        // When
        Optional<AdminUser> result = adminAuthService.authenticateByUsername("");

        // Then
        assertFalse(result.isPresent());
        verify(adminUserRepository, never()).findByUsernameAndIsActiveTrue(anyString());
    }

    @Test
    void authenticateByUsername_WithWhitespaceUsername_ShouldReturnEmpty() {
        // When
        Optional<AdminUser> result = adminAuthService.authenticateByUsername("   ");

        // Then
        assertFalse(result.isPresent());
        verify(adminUserRepository, never()).findByUsernameAndIsActiveTrue(anyString());
    }

    @Test
    void isValidApiKey_WithValidApiKey_ShouldReturnTrue() {
        // Given
        when(adminUserRepository.findByApiKeyAndIsActiveTrue("valid-api-key-123"))
                .thenReturn(Optional.of(mockAdminUser));

        // When
        boolean result = adminAuthService.isValidApiKey("valid-api-key-123");

        // Then
        assertTrue(result);
        verify(adminUserRepository).findByApiKeyAndIsActiveTrue("valid-api-key-123");
    }

    @Test
    void isValidApiKey_WithInvalidApiKey_ShouldReturnFalse() {
        // Given
        when(adminUserRepository.findByApiKeyAndIsActiveTrue("invalid-api-key"))
                .thenReturn(Optional.empty());

        // When
        boolean result = adminAuthService.isValidApiKey("invalid-api-key");

        // Then
        assertFalse(result);
        verify(adminUserRepository).findByApiKeyAndIsActiveTrue("invalid-api-key");
    }

    @Test
    void isValidApiKey_WithNullApiKey_ShouldReturnFalse() {
        // When
        boolean result = adminAuthService.isValidApiKey(null);

        // Then
        assertFalse(result);
        verify(adminUserRepository, never()).findByApiKeyAndIsActiveTrue(anyString());
    }

    @Test
    void isValidApiKey_WithEmptyApiKey_ShouldReturnFalse() {
        // When
        boolean result = adminAuthService.isValidApiKey("");

        // Then
        assertFalse(result);
        verify(adminUserRepository, never()).findByApiKeyAndIsActiveTrue(anyString());
    }

    @Test
    void authenticateByApiKey_WithInactiveUser_ShouldReturnEmpty() {
        // Given - Repository returns empty for inactive user
        when(adminUserRepository.findByApiKeyAndIsActiveTrue("inactive-user-key"))
                .thenReturn(Optional.empty());

        // When
        Optional<AdminUser> result = adminAuthService.authenticateByApiKey("inactive-user-key");

        // Then
        assertFalse(result.isPresent());
        verify(adminUserRepository).findByApiKeyAndIsActiveTrue("inactive-user-key");
    }

    @Test
    void authenticateByUsername_WithInactiveUser_ShouldReturnEmpty() {
        // Given - Repository returns empty for inactive user
        when(adminUserRepository.findByUsernameAndIsActiveTrue("inactive-user"))
                .thenReturn(Optional.empty());

        // When
        Optional<AdminUser> result = adminAuthService.authenticateByUsername("inactive-user");

        // Then
        assertFalse(result.isPresent());
        verify(adminUserRepository).findByUsernameAndIsActiveTrue("inactive-user");
    }

    @Test
    void authenticateByApiKey_WithSpecialCharacters_ShouldWork() {
        // Given
        String specialApiKey = "api-key-with-special-chars-!@#$%";
        when(adminUserRepository.findByApiKeyAndIsActiveTrue(specialApiKey))
                .thenReturn(Optional.of(mockAdminUser));

        // When
        Optional<AdminUser> result = adminAuthService.authenticateByApiKey(specialApiKey);

        // Then
        assertTrue(result.isPresent());
        verify(adminUserRepository).findByApiKeyAndIsActiveTrue(specialApiKey);
    }

    @Test
    void authenticateByUsername_WithSpecialCharacters_ShouldWork() {
        // Given
        String specialUsername = "admin.user_123";
        when(adminUserRepository.findByUsernameAndIsActiveTrue(specialUsername))
                .thenReturn(Optional.of(mockAdminUser));

        // When
        Optional<AdminUser> result = adminAuthService.authenticateByUsername(specialUsername);

        // Then
        assertTrue(result.isPresent());
        verify(adminUserRepository).findByUsernameAndIsActiveTrue(specialUsername);
    }
}