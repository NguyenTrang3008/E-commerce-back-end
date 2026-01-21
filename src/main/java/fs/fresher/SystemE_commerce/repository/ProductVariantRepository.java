package fs.fresher.SystemE_commerce.repository;

import fs.fresher.SystemE_commerce.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductIdAndIsActiveTrue(Long productId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.id = :id")
    Optional<ProductVariant> findByIdWithLock(@Param("id") Long id);
    
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.stockQuantity - pv.reservedQuantity >= :minStock AND pv.isActive = true")
    List<ProductVariant> findAvailableVariants(@Param("minStock") Integer minStock);
}