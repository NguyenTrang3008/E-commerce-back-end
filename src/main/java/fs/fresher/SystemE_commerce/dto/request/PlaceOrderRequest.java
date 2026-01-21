package fs.fresher.SystemE_commerce.dto.request;

import fs.fresher.SystemE_commerce.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String shippingAddress;
    private String note;
    private PaymentMethod paymentMethod;
    
    // Optional - if using checkout session
    private String checkoutToken;
    
    // Optional - if placing order directly from cart
    private String cartId;
}