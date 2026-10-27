package uz.epay.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import uz.epay.ApiTestBase;
import uz.epay.entity.Product;
import uz.epay.entity.ProductStatus;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(roles = "WAREHOUSE")
class WarehouseProductTest extends ApiTestBase {
    Product product;

    @BeforeEach
    void seed() {
        product = product("W-1", "Mahsulot", category("Kategoriya", 0), null, ProductStatus.DRAFT);
    }

    Map<String, Object> body(String price, String status) {
        Map<String, Object> b = new HashMap<>();
        b.put("sku", "W-1");
        b.put("name", "Yangi nom");
        b.put("categoryId", product.getCategory().getId());
        b.put("price", price);
        b.put("status", status);
        b.put("version", productRepository.findById(product.getId()).orElseThrow().getVersion());
        return b;
    }

    @Test
    void canEditAndPublishWithoutChangingPrice() throws Exception {
        mvc.perform(put("/api/admin/products/{id}", product.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body("100.00", "PUBLISHED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Yangi nom"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void samePriceWithDifferentScaleIsNotAChange() throws Exception {
        mvc.perform(put("/api/admin/products/{id}", product.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body("100", "DRAFT"))))
                .andExpect(status().isOk());
    }

    @Test
    void cannotChangePriceByDefault() throws Exception {
        mvc.perform(put("/api/admin/products/{id}", product.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body("99.99", "DRAFT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Narxni o'zgartirish huquqingiz yo'q"));
    }

    @Test
    void cannotArchive() throws Exception {
        mvc.perform(put("/api/admin/products/{id}", product.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body("100.00", "ARCHIVED"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanChangePrice() throws Exception {
        mvc.perform(put("/api/admin/products/{id}", product.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body("99.99", "ARCHIVED"))))
                .andExpect(status().isOk());
    }

    @Nested
    @TestPropertySource(properties = "epay.security.warehouse-can-edit-price=true")
    class WhenPriceEditingAllowed {
        @Test
        void canChangePrice() throws Exception {
            mvc.perform(put("/api/admin/products/{id}", product.getId()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(toJson(body("99.99", "PUBLISHED"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.price").value(99.99));
        }
    }
}
