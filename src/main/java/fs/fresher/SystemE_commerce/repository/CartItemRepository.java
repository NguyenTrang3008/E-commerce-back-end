package fs.fresher.SystemE_commerce.repository;

import fs.fresher.SystemE_commerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductVariantId(Long cartId, Long skuId);
    
    @Modifying
    @Transactional
    void deleteByCartIdAndProductVariantId(Long cartId, Long skuId);
}