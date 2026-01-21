package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.dto.response.PagedResponse;
import fs.fresher.SystemE_commerce.dto.response.ProductDetailResponse;
import fs.fresher.SystemE_commerce.dto.response.ProductFilterOptionsResponse;
import fs.fresher.SystemE_commerce.dto.response.ProductListResponse;
import fs.fresher.SystemE_commerce.entity.Category;
import fs.fresher.SystemE_commerce.entity.Product;
import fs.fresher.SystemE_commerce.entity.ProductVariant;
import fs.fresher.SystemE_commerce.exception.ResourceNotFoundException;
import fs.fresher.SystemE_commerce.repository.CategoryRepository;
import fs.fresher.SystemE_commerce.repository.ProductRepository;
import fs.fresher.SystemE_commerce.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    
    public PagedResponse<ProductListResponse> getProducts(
            int page, int size, List<Long> categoryIds, 
            BigDecimal minPrice, BigDecimal maxPrice, 
            List<String> colors, List<String> sizes, String search) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            
            Page<Product> productPage = productRepository.findProductsWithFilters(
                    categoryIds, minPrice, maxPrice, colors, sizes, search, pageable);
            
            List<ProductListResponse> content = productPage.getContent().stream()
                    .map(this::mapToProductListResponse)
                    .collect(Collectors.toList());
            
            return new PagedResponse<>(
                    content,
                    productPage.getNumber(),
                    productPage.getSize(),
                    productPage.getTotalElements(),
                    productPage.getTotalPages(),
                    productPage.isFirst(),
                    productPage.isLast(),
                    productPage.hasNext(),
                    productPage.hasPrevious()
            );
        } catch (Exception e) {
            // Log the actual error for debugging
            log.error("Error getting products: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve products: " + e.getMessage(), e);
        }
    }
    
    public ProductFilterOptionsResponse getFilterOptions(List<Long> categoryIds) {
        List<String> availableColors = productRepository.findAvailableColorsByCategories(categoryIds);
        List<String> availableSizes = productRepository.findAvailableSizesByCategories(categoryIds);
        BigDecimal minPrice = productRepository.findMinPriceByCategories(categoryIds);
        BigDecimal maxPrice = productRepository.findMaxPriceByCategories(categoryIds);
        
        List<Category> categories = categoryRepository.findAll(Sort.by("name"));
        List<ProductFilterOptionsResponse.CategoryOption> categoryOptions = categories.stream()
                .map(cat -> new ProductFilterOptionsResponse.CategoryOption(cat.getId(), cat.getName()))
                .collect(Collectors.toList());
        
        return new ProductFilterOptionsResponse(
                categoryOptions,
                availableColors,
                availableSizes,
                minPrice != null ? minPrice : BigDecimal.ZERO,
                maxPrice != null ? maxPrice : BigDecimal.ZERO
        );
    }
    
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> ResourceNotFoundException.product(productId));
        
        List<ProductVariant> variants = productVariantRepository.findByProductIdAndIsActiveTrue(productId);
        
        return mapToProductDetailResponse(product, variants);
    }
    
    private ProductListResponse mapToProductListResponse(Product product) {
        // Sử dụng repository để lấy variants thay vì lazy loading
        List<ProductVariant> variants = productVariantRepository.findByProductIdAndIsActiveTrue(product.getId());
        
        BigDecimal minPrice = variants.stream()
                .map(ProductVariant::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(product.getBasePrice());
        
        BigDecimal maxPrice = variants.stream()
                .map(ProductVariant::getPrice)
                .max(BigDecimal::compareTo)
                .orElse(product.getBasePrice());
        
        return new ProductListResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getImageUrl(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getCreatedAt(),
                variants.size(),
                minPrice,
                maxPrice
        );
    }
    
    private ProductDetailResponse mapToProductDetailResponse(Product product, List<ProductVariant> variants) {
        List<ProductDetailResponse.ProductVariantResponse> variantResponses = variants.stream()
                .map(variant -> new ProductDetailResponse.ProductVariantResponse(
                        variant.getId(),
                        variant.getSku(),
                        variant.getSize(),
                        variant.getColor(),
                        variant.getPrice(),
                        variant.getAvailableStock(),
                        variant.getIsActive()
                ))
                .collect(Collectors.toList());
        
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getImageUrl(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getCreatedAt(),
                variantResponses
        );
    }
}