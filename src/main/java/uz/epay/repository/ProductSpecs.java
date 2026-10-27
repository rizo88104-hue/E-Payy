package uz.epay.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import uz.epay.entity.Product;
import uz.epay.entity.ProductStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Katalog filtrlari. Criteria API bilan: JPQL dagi "(:q is null or ...)" PostgreSQL'da
 * "could not determine data type of parameter" xatosini beradi.
 */
public final class ProductSpecs {
    private static final char ESCAPE = '\\';

    private ProductSpecs() {
    }

    /** Har bir parametr null bo'lsa — shu filtr qo'llanmaydi. q — nom yoki SKU ichida, harf katta-kichikligisiz. */
    public static Specification<Product> filter(String q, Long categoryId, Long brandId, ProductStatus status) {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String pattern = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                where.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, ESCAPE),
                        cb.like(cb.lower(root.get("sku")), pattern, ESCAPE)));
            }
            if (categoryId != null) {
                where.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (brandId != null) {
                where.add(cb.equal(root.get("brand").get("id"), brandId));
            }
            if (status != null) {
                where.add(cb.equal(root.get("status"), status));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }

    /** Foydalanuvchi kiritgan % va _ maxsus belgi bo'lib qolmasin. */
    static String escapeLike(String s) {
        String e = String.valueOf(ESCAPE);
        return s.replace(e, e + e).replace("%", e + "%").replace("_", e + "_");
    }
}
