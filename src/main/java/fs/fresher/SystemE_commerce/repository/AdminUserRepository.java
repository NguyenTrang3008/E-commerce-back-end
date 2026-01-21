package fs.fresher.SystemE_commerce.repository;

import fs.fresher.SystemE_commerce.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByApiKeyAndIsActiveTrue(String apiKey);
    Optional<AdminUser> findByUsernameAndIsActiveTrue(String username);
}