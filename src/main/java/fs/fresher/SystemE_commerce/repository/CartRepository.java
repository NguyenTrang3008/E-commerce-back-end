package fs.fresher.SystemE_commerce.repository;

import fs.fresher.SystemE_commerce.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCartId(String cartId);
    
    Optional<Cart> findBySessionToken(String sessionToken);
    
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.productVariant pv LEFT JOIN FETCH pv.product WHERE c.cartId = :cartId")
    Optional<Cart> findByCartIdWithItems(@Param("cartId") String cartId);
    
    @Query("SELECT c FROM Cart c WHERE c.expiresAt < :now")
    List<Cart> findExpiredCarts(@Param("now") LocalDateTime now);
    
    @Query("SELECT c FROM Cart c WHERE c.lastAccessedAt < :cutoffTime")
    List<Cart> findInactiveCarts(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT c FROM Cart c WHERE c.items IS EMPTY OR SIZE(c.items) = 0")
    List<Cart> findEmptyCarts();
    
    @Query("SELECT c FROM Cart c WHERE (c.items IS EMPTY OR SIZE(c.items) = 0) AND c.lastAccessedAt < :cutoffTime")
    List<Cart> findEmptyInactiveCarts(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Modifying
    @Query("DELETE FROM Cart c WHERE c.expiresAt < :now")
    int deleteExpiredCarts(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM Cart c WHERE (c.items IS EMPTY OR SIZE(c.items) = 0) AND c.lastAccessedAt < :cutoffTime")
    int deleteEmptyInactiveCarts(@Param("cutoffTime") LocalDateTime cutoffTime);
}