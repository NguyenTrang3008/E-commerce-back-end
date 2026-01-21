package fs.fresher.SystemE_commerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterOptionsResponse {
    private List<CategoryOption> categories;
    private List<String> availableColors;
    private List<String> availableSizes;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryOption {
        private Long id;
        private String name;
    }
}
