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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Product mockProduct;
    private Category mockCategory;
    private ProductVariant mockVariant1;
    private ProductVariant mockVariant2;
    private List<ProductVariant> mockVariants;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        // Setup Category
        mockCategory = new Category();
        mockCategory.setId(1L);
        mockCategory.setName("Electronics");

        // Setup Product
        mockProduct = new Product();
        mockProduct.setId(1L);
        mockProduct.setName("Test Product");
        mockProduct.setDescription("Test Description");
        mockProduct.setBasePrice(new BigDecimal("100.00"));
        mockProduct.setImageUrl("http://example.com/image.jpg");
        mockProduct.setCategory(mockCategory);
        mockProduct.setCreatedAt(LocalDateTime.now());

        // Setup ProductVariants
        mockVariant1 = new ProductVariant();
        mockVariant1.setId(1L);
        mockVariant1.setProduct(mockProduct);
        mockVariant1.setSku("TEST-SKU-001");
        mockVariant1.setSize("M");
        mockVariant1.setColor("Red");
        mockVariant1.setPrice(new BigDecimal("90.00"));
        mockVariant1.setStockQuantity(10);
        mockVariant1.setReservedQuantity(2);
        mockVariant1.setIsActive(true);

        mockVariant2 = new ProductVariant();
        mockVariant2.setId(2L);
        mockVariant2.setProduct(mockProduct);
        mockVariant2.setSku("TEST-SKU-002");
        mockVariant2.setSize("L");
        mockVariant2.setColor("Blue");
        mockVariant2.setPrice(new BigDecimal("110.00"));
        mockVariant2.setStockQuantity(15);
        mockVariant2.setReservedQuantity(3);
        mockVariant2.setIsActive(true);

        mockVariants = Arrays.asList(mockVariant1, mockVariant2);
    }

    @Test
    void getProducts_WithValidParameters_ShouldReturnPagedResponse() {
        // Given
        List<Product> products = Arrays.asList(mockProduct);
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productRepository.findProductsWithFilters(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(productPage);
        when(productVariantRepository.findByProductIdAndIsActiveTrue(1L))
                .thenReturn(mockVariants);

        // When
        PagedResponse<ProductListResponse> result = productService.getProducts(
                0, 10, Arrays.asList(1L), 
                new BigDecimal("50.00"), new BigDecimal("200.00"),
                Arrays.asList("Red"), Arrays.asList("M"), "test");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        
        ProductListResponse productResponse = result.getContent().get(0);
        assertEquals(mockProduct.getId(), productResponse.getId());
        assertEquals(mockProduct.getName(), productResponse.getName());
        assertEquals(mockProduct.getDescription(), productResponse.getDescription());
        assertEquals(mockCategory.getName(), productResponse.getCategoryName());
        assertEquals(2, productResponse.getTotalVariants());
        assertEquals(new BigDecimal("90.00"), productResponse.getMinPrice());
        assertEquals(new BigDecimal("110.00"), productResponse.getMaxPrice());

        verify(productRepository).findProductsWithFilters(
                eq(Arrays.asList(1L)), eq(new BigDecimal("50.00")), eq(new BigDecimal("200.00")),
                eq(Arrays.asList("Red")), eq(Arrays.asList("M")), eq("test"), any(Pageable.class));
        verify(productVariantRepository).findByProductIdAndIsActiveTrue(1L);
    }

    @Test
    void getProducts_WithNullFilters_ShouldReturnAllProducts() {
        // Given
        List<Product> products = Arrays.asList(mockProduct);
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productRepository.findProductsWithFilters(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(productPage);
        when(productVariantRepository.findByProductIdAndIsActiveTrue(1L))
                .thenReturn(mockVariants);

        // When
        PagedResponse<ProductListResponse> result = productService.getProducts(
                0, 10, null, null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        
        verify(productRepository).findProductsWithFilters(
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void getProducts_WithEmptyResult_ShouldReturnEmptyPage() {
        // Given
        Page<Product> emptyPage = new PageImpl<>(Arrays.asList());
        
        when(productRepository.findProductsWithFilters(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        PagedResponse<ProductListResponse> result = productService.getProducts(
                0, 10, null, null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
    }

    @Test
    void getProducts_WithRepositoryException_ShouldThrowRuntimeException() {
        // Given
        when(productRepository.findProductsWithFilters(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productService.getProducts(0, 10, null, null, null, null, null, null);
        });

        assertTrue(exception.getMessage().contains("Failed to retrieve products"));
        assertTrue(exception.getCause().getMessage().contains("Database error"));
    }

    @Test
    void getFilterOptions_WithCategoryIds_ShouldReturnFilterOptions() {
        // Given
        List<Long> categoryIds = Arrays.asList(1L);
        List<String> colors = Arrays.asList("Red", "Blue", "Green");
        List<String> sizes = Arrays.asList("S", "M", "L", "XL");
        BigDecimal minPrice = new BigDecimal("50.00");
        BigDecimal maxPrice = new BigDecimal("200.00");
        List<Category> categories = Arrays.asList(mockCategory);

        when(productRepository.findAvailableColorsByCategories(categoryIds))
                .thenReturn(colors);
        when(productRepository.findAvailableSizesByCategories(categoryIds))
                .thenReturn(sizes);
        when(productRepository.findMinPriceByCategories(categoryIds))
                .thenReturn(minPrice);
        when(productRepository.findMaxPriceByCategories(categoryIds))
                .thenReturn(maxPrice);
        when(categoryRepository.findAll(any(Sort.class)))
                .thenReturn(categories);

        // When
        ProductFilterOptionsResponse result = productService.getFilterOptions(categoryIds);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getCategories().size());
        assertEquals("Electronics", result.getCategories().get(0).getName());
        assertEquals(3, result.getAvailableColors().size()); // Mock returns 3 colors
        assertTrue(result.getAvailableColors().contains("Red"));
        assertEquals(4, result.getAvailableSizes().size());
        assertTrue(result.getAvailableSizes().contains("M"));
        assertEquals(minPrice, result.getMinPrice());
        assertEquals(maxPrice, result.getMaxPrice());

        verify(productRepository).findAvailableColorsByCategories(categoryIds);
        verify(productRepository).findAvailableSizesByCategories(categoryIds);
        verify(productRepository).findMinPriceByCategories(categoryIds);
        verify(productRepository).findMaxPriceByCategories(categoryIds);
        verify(categoryRepository).findAll(any(Sort.class));
    }

    @Test
    void getFilterOptions_WithNullPrices_ShouldReturnZeroAsDefault() {
        // Given
        List<Long> categoryIds = Arrays.asList(1L);
        
        when(productRepository.findAvailableColorsByCategories(categoryIds))
                .thenReturn(Arrays.asList());
        when(productRepository.findAvailableSizesByCategories(categoryIds))
                .thenReturn(Arrays.asList());
        when(productRepository.findMinPriceByCategories(categoryIds))
                .thenReturn(null);
        when(productRepository.findMaxPriceByCategories(categoryIds))
                .thenReturn(null);
        when(categoryRepository.findAll(any(Sort.class)))
                .thenReturn(Arrays.asList());

        // When
        ProductFilterOptionsResponse result = productService.getFilterOptions(categoryIds);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getMinPrice());
        assertEquals(BigDecimal.ZERO, result.getMaxPrice());
    }

    @Test
    void getProductDetail_WithValidProductId_ShouldReturnProductDetail() {
        // Given
        when(productRepository.findByIdWithCategory(1L))
                .thenReturn(Optional.of(mockProduct));
        when(productVariantRepository.findByProductIdAndIsActiveTrue(1L))
                .thenReturn(mockVariants);

        // When
        ProductDetailResponse result = productService.getProductDetail(1L);

        // Then
        assertNotNull(result);
        assertEquals(mockProduct.getId(), result.getId());
        assertEquals(mockProduct.getName(), result.getName());
        assertEquals(mockProduct.getDescription(), result.getDescription());
        assertEquals(mockProduct.getBasePrice(), result.getBasePrice());
        assertEquals(mockCategory.getName(), result.getCategoryName());
        assertEquals(2, result.getVariants().size());

        ProductDetailResponse.ProductVariantResponse variant1Response = result.getVariants().get(0);
        assertEquals(mockVariant1.getId(), variant1Response.getId());
        assertEquals(mockVariant1.getSku(), variant1Response.getSku());
        assertEquals(mockVariant1.getSize(), variant1Response.getSize());
        assertEquals(mockVariant1.getColor(), variant1Response.getColor());
        assertEquals(mockVariant1.getPrice(), variant1Response.getPrice());

        verify(productRepository).findByIdWithCategory(1L);
        verify(productVariantRepository).findByProductIdAndIsActiveTrue(1L);
    }

    @Test
    void getProductDetail_WithNonExistentProductId_ShouldThrowResourceNotFoundException() {
        // Given
        when(productRepository.findByIdWithCategory(999L))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProductDetail(999L);
        });

        assertNotNull(exception);
        verify(productRepository).findByIdWithCategory(999L);
        verify(productVariantRepository, never()).findByProductIdAndIsActiveTrue(any());
    }

    @Test
    void getProductDetail_WithNoVariants_ShouldReturnEmptyVariantsList() {
        // Given
        when(productRepository.findByIdWithCategory(1L))
                .thenReturn(Optional.of(mockProduct));
        when(productVariantRepository.findByProductIdAndIsActiveTrue(1L))
                .thenReturn(Arrays.asList());

        // When
        ProductDetailResponse result = productService.getProductDetail(1L);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getVariants().size());
        verify(productVariantRepository).findByProductIdAndIsActiveTrue(1L);
    }

    @Test
    void getProducts_WithProductWithoutCategory_ShouldHandleNullCategory() {
        // Given
        mockProduct.setCategory(null);
        List<Product> products = Arrays.asList(mockProduct);
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productRepository.findProductsWithFilters(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(productPage);
        when(productVariantRepository.findByProductIdAndIsActiveTrue(1L))
                .thenReturn(mockVariants);

        // When
        PagedResponse<ProductListResponse> result = productService.getProducts(
                0, 10, null, null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        
        ProductListResponse productResponse = result.getContent().get(0);
        assertNull(productResponse.getCategoryName());
    }

    @Test
    void getProducts_WithProductWithoutVariants_ShouldUseBasePrice() {
        // Given
        List<Product> products = Arrays.asList(mockProduct);
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productRepository.findProductsWithFilters(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(productPage);
        when(productVariantRepository.findByProductIdAndIsActiveTrue(1L))
                .thenReturn(Arrays.asList()); // No variants

        // When
        PagedResponse<ProductListResponse> result = productService.getProducts(
                0, 10, null, null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        
        ProductListResponse productResponse = result.getContent().get(0);
        assertEquals(0, productResponse.getTotalVariants());
        assertEquals(mockProduct.getBasePrice(), productResponse.getMinPrice());
        assertEquals(mockProduct.getBasePrice(), productResponse.getMaxPrice());
    }

    @Test
    void getFilterOptions_WithEmptyCategories_ShouldReturnEmptyOptions() {
        // Given
        List<Long> categoryIds = Arrays.asList(1L);
        
        when(productRepository.findAvailableColorsByCategories(categoryIds))
                .thenReturn(Arrays.asList());
        when(productRepository.findAvailableSizesByCategories(categoryIds))
                .thenReturn(Arrays.asList());
        when(productRepository.findMinPriceByCategories(categoryIds))
                .thenReturn(new BigDecimal("0.00"));
        when(productRepository.findMaxPriceByCategories(categoryIds))
                .thenReturn(new BigDecimal("0.00"));
        when(categoryRepository.findAll(any(Sort.class)))
                .thenReturn(Arrays.asList());

        // When
        ProductFilterOptionsResponse result = productService.getFilterOptions(categoryIds);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCategories().size());
        assertEquals(0, result.getAvailableColors().size());
        assertEquals(0, result.getAvailableSizes().size());
        assertEquals(new BigDecimal("0.00"), result.getMinPrice());
        assertEquals(new BigDecimal("0.00"), result.getMaxPrice());
    }

    @Test
    void getProductDetail_WithProductWithoutCategory_ShouldHandleNullCategory() {
        // Given
        mockProduct.setCategory(null);
        when(productRepository.findByIdWithCategory(1L))
                .thenReturn(Optional.of(mockProduct));
        when(productVariantRepository.findByProductIdAndIsActiveTrue(1L))
                .thenReturn(mockVariants);

        // When
        ProductDetailResponse result = productService.getProductDetail(1L);

        // Then
        assertNotNull(result);
        assertNull(result.getCategoryName());
        assertEquals(mockProduct.getName(), result.getName());
    }

    @Test
    void getProducts_WithLargePageSize_ShouldHandleCorrectly() {
        // Given
        List<Product> products = Arrays.asList(mockProduct);
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productRepository.findProductsWithFilters(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(productPage);
        when(productVariantRepository.findByProductIdAndIsActiveTrue(1L))
                .thenReturn(mockVariants);

        // When
        PagedResponse<ProductListResponse> result = productService.getProducts(
                0, 100, null, null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getPage());
        assertEquals(1, result.getSize()); // Page size is actual content size, not requested size
    }

    @Test
    void getFilterOptions_WithNullCategoryIds_ShouldWork() {
        // Given
        when(productRepository.findAvailableColorsByCategories(null))
                .thenReturn(Arrays.asList("Red", "Blue"));
        when(productRepository.findAvailableSizesByCategories(null))
                .thenReturn(Arrays.asList("M", "L"));
        when(productRepository.findMinPriceByCategories(null))
                .thenReturn(new BigDecimal("10.00"));
        when(productRepository.findMaxPriceByCategories(null))
                .thenReturn(new BigDecimal("500.00"));
        when(categoryRepository.findAll(any(Sort.class)))
                .thenReturn(Arrays.asList(mockCategory));

        // When
        ProductFilterOptionsResponse result = productService.getFilterOptions(null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getCategories().size());
        assertEquals(2, result.getAvailableColors().size());
        assertEquals(2, result.getAvailableSizes().size());
        
        verify(productRepository).findAvailableColorsByCategories(null);
    }
}