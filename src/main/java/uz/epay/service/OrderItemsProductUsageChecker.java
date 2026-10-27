package uz.epay.service;

import org.springframework.stereotype.Component;
import uz.epay.repository.OrderItemRepository;

@Component
class OrderItemsProductUsageChecker implements ProductUsageChecker {
    private final OrderItemRepository orderItems;

    OrderItemsProductUsageChecker(OrderItemRepository orderItems) {
        this.orderItems = orderItems;
    }

    @Override
    public boolean isUsedInOrders(Long productId) {
        return orderItems.existsByProductId(productId);
    }
}
