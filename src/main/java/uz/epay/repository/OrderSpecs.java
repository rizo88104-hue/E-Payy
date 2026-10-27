package uz.epay.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import uz.epay.entity.Order;
import uz.epay.entity.OrderStatus;
import uz.epay.entity.PaymentStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Xodimlar ro'yxati filtrlari (Criteria API — null parametrlar PostgreSQL'da muammo bermasin). */
public final class OrderSpecs {
    private static final char ESCAPE = '\\';

    private OrderSpecs() {
    }

    /**
     * @param from      shu vaqtdan (kiradi), null — chegarasiz
     * @param to        shu vaqtgacha (kirmaydi), null — chegarasiz
     * @param q         raqam, ism yoki telefon (raqamlari) ichida
     * @param visibleTo operator id: faqat biriktirilmagan va o'ziniki; null — hammasi
     */
    public static Specification<Order> filter(OrderStatus status, PaymentStatus payStatus, Instant from, Instant to,
                                              String q, Long visibleTo) {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (status != null) {
                where.add(cb.equal(root.get("status"), status));
            }
            if (payStatus != null) {
                where.add(cb.equal(root.get("payStatus"), payStatus));
            }
            if (from != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                where.add(cb.lessThan(root.get("createdAt"), to));
            }
            if (q != null && !q.isBlank()) {
                String text = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                List<Predicate> any = new ArrayList<>();
                any.add(cb.like(cb.lower(root.get("number")), text, ESCAPE));
                any.add(cb.like(cb.lower(root.get("customerName")), text, ESCAPE));
                String digits = q.replaceAll("[^0-9]", "");
                if (digits.length() >= 3) {
                    any.add(cb.like(root.get("phone"), "%" + digits + "%"));
                }
                where.add(cb.or(any.toArray(Predicate[]::new)));
            }
            if (visibleTo != null) {
                where.add(cb.or(cb.isNull(root.get("assignedOperator")),
                        cb.equal(root.get("assignedOperator").get("id"), visibleTo)));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }

    static String escapeLike(String s) {
        String e = String.valueOf(ESCAPE);
        return s.replace(e, e + e).replace("%", e + "%").replace("_", e + "_");
    }
}
