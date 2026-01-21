package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.entity.AdminUser;
import fs.fresher.SystemE_commerce.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminAuthService {
    
    private final AdminUserRepository adminUserRepository;
    
    public Optional<AdminUser> authenticateByApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return adminUserRepository.findByApiKeyAndIsActiveTrue(apiKey);
    }
    
    public Optional<AdminUser> authenticateByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return adminUserRepository.findByUsernameAndIsActiveTrue(username);
    }
    
    public boolean isValidApiKey(String apiKey) {
        return authenticateByApiKey(apiKey).isPresent();
    }
}