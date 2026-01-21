package fs.fresher.SystemE_commerce.controller;

import fs.fresher.SystemE_commerce.dto.response.PagedResponse;
import fs.fresher.SystemE_commerce.dto.response.ProductDetailResponse;
import fs.fresher.SystemE_commerce.dto.response.ProductFilterOptionsResponse;
import fs.fresher.SystemE_commerce.dto.response.ProductListResponse;
import fs.fresher.SystemE_commerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog endpoints")
public class ProductController {
    
    private final ProductService productService;
    
    @GetMapping
    @Operation(
        summary = "Get products with filters",
        description = "Get paginated list of products with optional filters (category, price, color, size, search)"
    )
    public ResponseEntity<PagedResponse<ProductListResponse>> getProducts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Category IDs to filter") @RequestParam(required = false) List<Long> categoryIds,
            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Colors to filter") @RequestParam(required = false) List<String> colors,
            @Parameter(description = "Sizes to filter") @RequestParam(required = false) List<String> sizes,
            @Parameter(description = "Search by product name") @RequestParam(required = false) String search) {
        
        PagedResponse<ProductListResponse> response = productService.getProducts(
                page, size, categoryIds, minPrice, maxPrice, colors, sizes, search);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/filter-options")
    @Operation(
        summary = "Get filter options",
        description = "Get available categories, colors, sizes, and price range for filtering"
    )
    public ResponseEntity<ProductFilterOptionsResponse> getFilterOptions(
            @Parameter(description = "Category IDs to get filtered options") 
            @RequestParam(required = false) List<Long> categoryIds) {
        ProductFilterOptionsResponse response = productService.getFilterOptions(categoryIds);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{productId}")
    @Operation(
        summary = "Get product detail",
        description = "Get detailed product information including all variants"
    )
    public ResponseEntity<ProductDetailResponse> getProductDetail(
            @Parameter(description = "Product ID") @PathVariable Long productId) {
        ProductDetailResponse response = productService.getProductDetail(productId);
        return ResponseEntity.ok(response);
    }
}