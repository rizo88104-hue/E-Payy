package uz.epay.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import uz.epay.entity.Order;
import uz.epay.entity.OrderStatus;
import uz.epay.entity.PaymentStatus;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    /** Buyurtma raqami uchun: yagona ketma-ketlik (parallel buyurtmalarda ham takrorlanmaydi). */
    @Query(value = "select nextval('order_number_seq')", nativeQuery = true)
    long nextNumber();

    @Override
    @EntityGraph(attributePaths = "assignedOperator")
    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "assignedOperator"})
    Optional<Order> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByNumber(String number);

    long countByPhoneAndStatusAndPayStatus(String phone, OrderStatus status, PaymentStatus payStatus);

    boolean existsByAssignedOperatorId(Long staffId);
}
