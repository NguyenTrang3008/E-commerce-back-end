package fs.fresher.SystemE_commerce.repository;

import fs.fresher.SystemE_commerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "LEFT JOIN p.variants v " +
           "WHERE p.isActive = true " +
           "AND (:categoryIds IS NULL OR p.category.id IN :categoryIds) " +
           "AND (:minPrice IS NULL OR p.basePrice >= :minPrice OR v.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice OR v.price <= :maxPrice) " +
           "AND (:colors IS NULL OR v.color IN :colors) " +
           "AND (:sizes IS NULL OR v.size IN :sizes) " +
           "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> findProductsWithFilters(
            @Param("categoryIds") List<Long> categoryIds,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("colors") List<String> colors,
            @Param("sizes") List<String> sizes,
            @Param("search") String search,
            Pageable pageable
    );
    
    @Query("SELECT DISTINCT v.color FROM Product p " +
           "JOIN p.variants v " +
           "WHERE p.isActive = true AND v.isActive = true AND v.color IS NOT NULL " +
           "AND (:categoryIds IS NULL OR p.category.id IN :categoryIds) " +
           "ORDER BY v.color")
    List<String> findAvailableColorsByCategories(@Param("categoryIds") List<Long> categoryIds);
    
    @Query("SELECT DISTINCT v.size FROM Product p " +
           "JOIN p.variants v " +
           "WHERE p.isActive = true AND v.isActive = true AND v.size IS NOT NULL " +
           "AND (:categoryIds IS NULL OR p.category.id IN :categoryIds) " +
           "ORDER BY v.size")
    List<String> findAvailableSizesByCategories(@Param("categoryIds") List<Long> categoryIds);
    
    @Query("SELECT MIN(COALESCE(v.price, p.basePrice)) FROM Product p " +
           "LEFT JOIN p.variants v " +
           "WHERE p.isActive = true " +
           "AND (:categoryIds IS NULL OR p.category.id IN :categoryIds)")
    BigDecimal findMinPriceByCategories(@Param("categoryIds") List<Long> categoryIds);
    
    @Query("SELECT MAX(COALESCE(v.price, p.basePrice)) FROM Product p " +
           "LEFT JOIN p.variants v " +
           "WHERE p.isActive = true " +
           "AND (:categoryIds IS NULL OR p.category.id IN :categoryIds)")
    BigDecimal findMaxPriceByCategories(@Param("categoryIds") List<Long> categoryIds);
    
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id = :id")
    java.util.Optional<Product> findByIdWithCategory(@Param("id") Long id);
}