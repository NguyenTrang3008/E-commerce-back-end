package fs.fresher.SystemE_commerce.service;

import fs.fresher.SystemE_commerce.enums.OrderStatus;
import fs.fresher.SystemE_commerce.enums.PaymentMethod;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OrderStateMachineService {
    
    private static final Map<OrderStatus, Set<OrderStatus>> STATE_TRANSITIONS = new HashMap<>();
    
    static {
        // AWAITING_PAYMENT transitions
        STATE_TRANSITIONS.put(OrderStatus.AWAITING_PAYMENT, Set.of(
                OrderStatus.PAID,
                OrderStatus.CANCELLED
        ));
        
        // CONFIRMED (COD) transitions
        STATE_TRANSITIONS.put(OrderStatus.CONFIRMED, Set.of(
                OrderStatus.PROCESSING,
                OrderStatus.SHIPPING,
                OrderStatus.CANCELLED
        ));
        
        // PAID transitions
        STATE_TRANSITIONS.put(OrderStatus.PAID, Set.of(
                OrderStatus.PROCESSING,
                OrderStatus.SHIPPING,
                OrderStatus.CANCELLED
        ));
        
        // PROCESSING transitions
        STATE_TRANSITIONS.put(OrderStatus.PROCESSING, Set.of(
                OrderStatus.SHIPPING,
                OrderStatus.CANCELLED
        ));
        
        // SHIPPING transitions
        STATE_TRANSITIONS.put(OrderStatus.SHIPPING, Set.of(
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED
        ));
        
        // SHIPPED transitions
        STATE_TRANSITIONS.put(OrderStatus.SHIPPED, Set.of(
                OrderStatus.DELIVERED
        ));
        
        // Terminal states (no transitions allowed)
        STATE_TRANSITIONS.put(OrderStatus.DELIVERED, Set.of());
        STATE_TRANSITIONS.put(OrderStatus.CANCELLED, Set.of());
    }
    
    /**
     * Check if status transition is valid
     */
    public boolean isValidTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        
        if (currentStatus == newStatus) {
            return false; // No self-transitions
        }
        
        Set<OrderStatus> allowedTransitions = STATE_TRANSITIONS.get(currentStatus);
        return allowedTransitions != null && allowedTransitions.contains(newStatus);
    }
    
    /**
     * Get all valid next statuses for current status
     */
    public List<OrderStatus> getValidTransitions(OrderStatus currentStatus) {
        Set<OrderStatus> transitions = STATE_TRANSITIONS.get(currentStatus);
        return transitions != null ? new ArrayList<>(transitions) : new ArrayList<>();
    }
    
    /**
     * Check if order can be cancelled based on current status
     */
    public boolean canBeCancelled(OrderStatus currentStatus) {
        return getValidTransitions(currentStatus).contains(OrderStatus.CANCELLED);
    }
    
    /**
     * Get initial status based on payment method
     */
    public OrderStatus getInitialStatus(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.COD 
                ? OrderStatus.CONFIRMED 
                : OrderStatus.AWAITING_PAYMENT;
    }
    
    /**
     * Validate transition with business rules
     */
    public void validateTransition(OrderStatus currentStatus, OrderStatus newStatus, PaymentMethod paymentMethod) {
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new RuntimeException(String.format(
                    "Invalid status transition from %s to %s", 
                    currentStatus, newStatus));
        }
        
        // Additional business rules
        if (newStatus == OrderStatus.PAID && paymentMethod == PaymentMethod.COD) {
            throw new RuntimeException("COD orders cannot be marked as PAID");
        }
        
        if (newStatus == OrderStatus.SHIPPING && currentStatus == OrderStatus.AWAITING_PAYMENT) {
            throw new RuntimeException("Cannot ship unpaid orders");
        }
    }
    
    /**
     * Get human-readable transition description
     */
    public String getTransitionDescription(OrderStatus from, OrderStatus to) {
        return String.format("Status changed from %s to %s", 
                formatStatus(from), formatStatus(to));
    }
    
    private String formatStatus(OrderStatus status) {
        return status.name().toLowerCase().replace("_", " ");
    }
}