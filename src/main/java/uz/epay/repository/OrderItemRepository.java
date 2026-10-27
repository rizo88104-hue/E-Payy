package uz.epay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.epay.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByProductId(Long productId);
}
