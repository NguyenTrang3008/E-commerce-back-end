package fs.fresher.SystemE_commerce.repository;

import fs.fresher.SystemE_commerce.entity.CheckoutSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, Long> {
    Optional<CheckoutSession> findByCheckoutToken(String checkoutToken);
    
    @Query("SELECT cs FROM CheckoutSession cs WHERE cs.expiresAt < :now AND cs.isUsed = false")
    List<CheckoutSession> findExpiredSessions(LocalDateTime now);
    
    @Query("SELECT cs FROM CheckoutSession cs WHERE cs.expiresAt > :now AND cs.isUsed = false")
    List<CheckoutSession> findActiveSessions(LocalDateTime now);
}