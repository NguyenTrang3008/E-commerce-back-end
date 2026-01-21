package fs.fresher.SystemE_commerce.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class InventoryReserveRequest {
    
    @NotBlank(message = "Cart ID is required")
    private String cartId;
    
    // TTL is now fixed at 15 minutes and managed by backend
}