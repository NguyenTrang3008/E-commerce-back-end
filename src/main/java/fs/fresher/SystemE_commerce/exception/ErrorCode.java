package fs.fresher.SystemE_commerce.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enum chứa tất cả các mã lỗi và thông tin liên quan
 */
@Getter
public enum ErrorCode {
    // Validation Errors
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("INVALID_REQUEST", "Invalid request data", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD("MISSING_REQUIRED_FIELD", "Required field is missing", HttpStatus.BAD_REQUEST),
    
    // Resource Not Found Errors
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", "Resource already exists", HttpStatus.CONFLICT),
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "Product not found", HttpStatus.NOT_FOUND),
    PRODUCT_VARIANT_NOT_FOUND("PRODUCT_VARIANT_NOT_FOUND", "Product variant not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND("CATEGORY_NOT_FOUND", "Category not found", HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND),
    CART_NOT_FOUND("CART_NOT_FOUND", "Cart not found", HttpStatus.NOT_FOUND),
    CHECKOUT_SESSION_NOT_FOUND("CHECKOUT_SESSION_NOT_FOUND", "Checkout session not found", HttpStatus.NOT_FOUND),
    RESERVATION_NOT_FOUND("RESERVATION_NOT_FOUND", "Reservation not found", HttpStatus.NOT_FOUND),
    
    // Inventory Errors
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", "Insufficient stock available", HttpStatus.BAD_REQUEST),
    STOCK_RESERVATION_FAILED("STOCK_RESERVATION_FAILED", "Failed to reserve stock", HttpStatus.BAD_REQUEST),
    STOCK_RESERVATION_EXPIRED("STOCK_RESERVATION_EXPIRED", "Stock reservation has expired", HttpStatus.BAD_REQUEST),
    STOCK_ALREADY_CONFIRMED("STOCK_ALREADY_CONFIRMED", "Stock reservation already confirmed", HttpStatus.BAD_REQUEST),
    STOCK_ALREADY_RELEASED("STOCK_ALREADY_RELEASED", "Stock reservation already released", HttpStatus.BAD_REQUEST),
    
    // Cart Errors
    CART_ITEM_NOT_FOUND("CART_ITEM_NOT_FOUND", "Cart item not found", HttpStatus.NOT_FOUND),
    CART_EMPTY("CART_EMPTY", "Cart is empty", HttpStatus.BAD_REQUEST),
    INVALID_QUANTITY("INVALID_QUANTITY", "Invalid quantity specified", HttpStatus.BAD_REQUEST),
    
    // Order Errors
    ORDER_ALREADY_PLACED("ORDER_ALREADY_PLACED", "Order has already been placed", HttpStatus.BAD_REQUEST),
    ORDER_CANNOT_BE_CANCELLED("ORDER_CANNOT_BE_CANCELLED", "Order cannot be cancelled", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_STATUS("INVALID_ORDER_STATUS", "Invalid order status", HttpStatus.BAD_REQUEST),
    ORDER_PROCESSING_FAILED("ORDER_PROCESSING_FAILED", "Order processing failed", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // Payment Errors
    PAYMENT_FAILED("PAYMENT_FAILED", "Payment processing failed", HttpStatus.BAD_REQUEST),
    PAYMENT_URL_GENERATION_FAILED("PAYMENT_URL_GENERATION_FAILED", "Failed to generate payment URL", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_PAYMENT_STATUS("INVALID_PAYMENT_STATUS", "Invalid payment status", HttpStatus.BAD_REQUEST),
    PAYMENT_WEBHOOK_VERIFICATION_FAILED("PAYMENT_WEBHOOK_VERIFICATION_FAILED", "Payment webhook verification failed", HttpStatus.BAD_REQUEST),
    
    // Checkout Errors
    CHECKOUT_SESSION_EXPIRED("CHECKOUT_SESSION_EXPIRED", "Checkout session has expired", HttpStatus.BAD_REQUEST),
    CHECKOUT_ALREADY_COMPLETED("CHECKOUT_ALREADY_COMPLETED", "Checkout has already been completed", HttpStatus.BAD_REQUEST),
    
    // System Errors
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    DATABASE_ERROR("DATABASE_ERROR", "Database operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // Authentication & Authorization Errors
    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized access", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "Access forbidden", HttpStatus.FORBIDDEN),
    
    // Business Logic Errors
    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", "Business rule violation", HttpStatus.BAD_REQUEST),
    OPERATION_NOT_ALLOWED("OPERATION_NOT_ALLOWED", "Operation not allowed", HttpStatus.BAD_REQUEST);
    
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    
    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}