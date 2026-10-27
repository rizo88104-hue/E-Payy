package uz.epay;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import uz.epay.entity.Category;
import uz.epay.entity.Product;
import uz.epay.entity.ProductStatus;
import uz.epay.repository.CategoryRepository;
import uz.epay.repository.ProductRepository;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Haqiqiy PostgreSQL 17 da: barcha migratsiyalar bo'sh bazaga qo'llanadi va Hibernate sxemani tasdiqlaydi
 * (ddl-auto: validate — kontekst yuklansa, entity va jadvallar mos).
 * Docker bo'lmasa o'tkazib yuboriladi (lokal kompyuter); GitHub Actions runner'ida Docker bor — u yerda ishlaydi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MigrationPostgresIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    Flyway flyway;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    CategoryRepository categories;
    @Autowired
    ProductRepository products;

    @Test
    void migrationsApplyOnEmptyDatabase() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).extracting(m -> m.getVersion().getVersion()).containsExactly("1", "2", "3");
        assertThat(jdbc.queryForObject("select version()", String.class)).contains("PostgreSQL 17");
        // demo ma'lumotlar (db/dev-data) bu yerga tushmagan
        assertThat(jdbc.queryForObject("select count(*) from products", Integer.class)).isZero();
    }

    @Test
    void timestampsAreTimestamptzAndMoneyIsNumeric() {
        assertThat(jdbc.queryForObject("select data_type from information_schema.columns "
                + "where table_name = 'products' and column_name = 'created_at'", String.class))
                .isEqualTo("timestamp with time zone");
        assertThat(jdbc.queryForObject("select numeric_precision || ',' || numeric_scale from information_schema.columns "
                + "where table_name = 'products' and column_name = 'price'", String.class))
                .isEqualTo("14,2");
    }

    @Test
    void productRoundTrip() {
        Category c = new Category();
        c.setName("IT test");
        c = categories.save(c);
        Product p = new Product();
        p.setSku("IT-1");
        p.setName("Test");
        p.setCategory(c);
        p.setPrice(new BigDecimal("1234567890.12"));
        p.setStatus(ProductStatus.PUBLISHED);
        p.setSpecs("x".repeat(10_000));
        Long id = products.saveAndFlush(p).getId();

        Product back = products.findById(id).orElseThrow();
        assertThat(back.getPrice()).isEqualByComparingTo("1234567890.12");
        assertThat(back.getVersion()).isZero();
        assertThat(back.getCreatedAt()).isNotNull();
        products.deleteAll();
        categories.deleteAll();
    }
}
