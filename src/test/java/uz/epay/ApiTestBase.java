package uz.epay;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import uz.epay.entity.Brand;
import uz.epay.entity.Category;
import uz.epay.entity.Product;
import uz.epay.entity.ProductStatus;
import uz.epay.repository.BrandRepository;
import uz.epay.repository.CategoryRepository;
import uz.epay.repository.ProductRepository;

import java.math.BigDecimal;

/** API testlari uchun umumiy asos: test profili (H2, demo ma'lumotsiz), har testdan oldin toza katalog. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestBase {

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected CategoryRepository categoryRepository;
    @Autowired
    protected BrandRepository brandRepository;
    @Autowired
    protected ProductRepository productRepository;

    @BeforeEach
    void cleanCatalog() {
        jdbc.update("delete from product_images");
        jdbc.update("delete from products");
        jdbc.update("delete from brands");
        jdbc.update("delete from categories");
    }

    protected Category category(String name, int sortOrder) {
        Category c = new Category();
        c.setName(name);
        c.setSortOrder(sortOrder);
        return categoryRepository.save(c);
    }

    protected Brand brand(String name, String slug) {
        Brand b = new Brand();
        b.setName(name);
        b.setSlug(slug);
        return brandRepository.save(b);
    }

    protected Product product(String sku, String name, Category category, Brand brand, ProductStatus status) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setCategory(category);
        p.setBrand(brand);
        p.setPrice(new BigDecimal("100.00"));
        p.setStatus(status);
        return productRepository.save(p);
    }

    protected String toJson(Object value) {
        return json.writeValueAsString(value);
    }

    protected static long longAt(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), path)).longValue();
    }
}
