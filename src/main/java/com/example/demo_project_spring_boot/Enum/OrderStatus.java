package com.example.demo_project_spring_boot.Enum;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    PAID,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    REFUNDED,
    RETURNED;
    
    public String getDescription() {
        switch (this) {
            case PENDING: return "Order placed, awaiting confirmation";
            case CONFIRMED: return "Order confirmed by seller";
            case PROCESSING: return "Order is being processed";
            case PAID: return "Payment received";
            case SHIPPED: return "Order has been shipped";
            case OUT_FOR_DELIVERY: return "Out for delivery";
            case DELIVERED: return "Order delivered successfully";
            case COMPLETED: return "Order completed";
            case CANCELLED: return "Order cancelled";
            case REFUNDED: return "Order refunded";
            case RETURNED: return "Order returned";
            default: return "Unknown status";
        }
    }
    
    public boolean canTransitionTo(OrderStatus newStatus) {
        switch (this) {
            case PENDING:
                return newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED:
                return newStatus == PROCESSING || newStatus == CANCELLED;
            case PROCESSING:
                return newStatus == PAID || newStatus == CANCELLED;
            case PAID:
                return newStatus == SHIPPED || newStatus == REFUNDED;
            case SHIPPED:
                return newStatus == OUT_FOR_DELIVERY || newStatus == RETURNED;
            case OUT_FOR_DELIVERY:
                return newStatus == DELIVERED || newStatus == RETURNED;
            case DELIVERED:
                return newStatus == COMPLETED || newStatus == RETURNED;
            case COMPLETED:
            case CANCELLED:
            case REFUNDED:
            case RETURNED:
                return false; // Terminal states
            default:
                return false;
        }
    }
}
