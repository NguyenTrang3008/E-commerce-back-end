package fs.fresher.SystemE_commerce.configuration;

import fs.fresher.SystemE_commerce.entity.*;
import fs.fresher.SystemE_commerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1000) // Run after other initializations
public class DataInitializer implements CommandLineRunner {
    
    private final AdminUserRepository adminUserRepository;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Starting data initialization...");
            
            // Only initialize admin users, no sample data
            if (adminUserRepository.count() == 0) {
                log.info("Initializing admin users...");
                initializeAdminUsers();
                log.info("Admin users initialized successfully");
            } else {
                log.info("Admin users already exist, skipping initialization");
            }
            
            log.info("Data initialization completed!");
            
        } catch (Exception e) {
            log.error("Error during data initialization: {}", e.getMessage(), e);
        }
    }
    
    private void initializeAdminUsers() {
        try {
            // Create default admin user
            AdminUser admin = new AdminUser();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setApiKey(UUID.randomUUID().toString());
            admin.setRole("ADMIN");
            adminUserRepository.save(admin);
            
            log.info("Created admin user - username: admin, password: admin123");
            
            // Create staff user
            AdminUser staff = new AdminUser();
            staff.setUsername("staff");
            staff.setPassword("staff123");
            staff.setApiKey(UUID.randomUUID().toString());
            staff.setRole("STAFF");
            adminUserRepository.save(staff);
            
            log.info("Created staff user - username: staff, password: staff123");
            
        } catch (Exception e) {
            log.error("Error initializing admin users: {}", e.getMessage());
        }
    }
}