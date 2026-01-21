package fs.fresher.SystemE_commerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductListResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private String imageUrl;
    private String categoryName;
    private LocalDateTime createdAt;
    private Integer totalVariants;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}