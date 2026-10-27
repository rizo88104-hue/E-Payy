package uz.epay.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import uz.epay.ApiTestBase;
import uz.epay.entity.Brand;
import uz.epay.entity.Category;
import uz.epay.entity.ProductStatus;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Admin API. Rollar 1.3 da — hozircha istalgan kirgan foydalanuvchi (@WithMockUser). */
@WithMockUser(roles = "ADMIN")
class AdminProductApiTest extends ApiTestBase {
    private Category cpus;
    private Brand amd;

    @BeforeEach
    void seed() {
        cpus = category("Protsessorlar", 1);
        amd = brand("AMD", "amd");
    }

    private Map<String, Object> productBody(String sku) {
        Map<String, Object> body = new HashMap<>();
        body.put("sku", sku);
        body.put("name", "AMD Ryzen 5 5600X");
        body.put("categoryId", cpus.getId());
        body.put("brandId", amd.getId());
        body.put("price", "139.99");
        body.put("specs", "Yadro: 6");
        body.put("mxikCode", "08471001001000000");
        body.put("vatPercent", "12.00");
        return body;
    }

    private MvcResult create(Map<String, Object> body) throws Exception {
        return mvc.perform(post("/api/admin/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    /** @WithMockUser klass darajasida — anonymous() shu so'rov uchun uni bekor qiladi. */
    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/admin/products").with(anonymous())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/categories").with(anonymous()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReadUpdateDelete() throws Exception {
        MvcResult created = create(productBody("AMD-5600X"));
        long id = longAt(created, "$.id");
        mvc.perform(get("/api/admin/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.price").value(139.99))
                .andExpect(jsonPath("$.mxikCode").value("08471001001000000"))
                .andExpect(jsonPath("$.category.id").value(cpus.getId()))
                .andExpect(jsonPath("$.createdAt").exists());

        Map<String, Object> update = productBody("AMD-5600X");
        update.put("name", "AMD Ryzen 5 5600X BOX");
        update.put("status", "PUBLISHED");
        update.put("version", 0);
        mvc.perform(put("/api/admin/products/{id}", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("AMD Ryzen 5 5600X BOX"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.version").value(1));
        // endi ochiq katalogda ko'rinadi
        mvc.perform(get("/api/products/{id}", id)).andExpect(status().isOk());

        mvc.perform(delete("/api/admin/products/{id}", id).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("DELETED"));
        mvc.perform(get("/api/admin/products/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void staleVersionIs409() throws Exception {
        long id = longAt(create(productBody("AMD-5600X")), "$.id");
        Map<String, Object> first = productBody("AMD-5600X");
        first.put("version", 0);
        first.put("price", "129.99");
        mvc.perform(put("/api/admin/products/{id}", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(first)))
                .andExpect(status().isOk());

        // ikkinchi admin hali eski (0) version bilan saqlamoqchi
        Map<String, Object> second = productBody("AMD-5600X");
        second.put("version", 0);
        second.put("price", "99.99");
        mvc.perform(put("/api/admin/products/{id}", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("version"))
                .andExpect(jsonPath("$.detail").value("Boshqa joyda o'zgartirildi, sahifani yangilang"));
        // birinchisining narxi saqlanib qoldi
        mvc.perform(get("/api/admin/products/{id}", id)).andExpect(jsonPath("$.price").value(129.99));
    }

    @Test
    void updateWithoutVersionIs400() throws Exception {
        long id = longAt(create(productBody("AMD-5600X")), "$.id");
        mvc.perform(put("/api/admin/products/{id}", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(productBody("AMD-5600X"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateSkuIs409() throws Exception {
        create(productBody("AMD-5600X"));
        mvc.perform(post("/api/admin/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(productBody("AMD-5600X"))))
                .andExpect(status().isConflict());
    }

    @Test
    void validation() throws Exception {
        Map<String, Object> body = productBody("bad sku!");
        body.put("price", "-1");
        body.put("mxikCode", "123");
        body.put("vatPercent", "150");
        mvc.perform(post("/api/admin/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("sku")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("price")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("mxikCode")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("vatPercent")));
    }

    @Test
    void unknownCategoryIs400() throws Exception {
        Map<String, Object> body = productBody("AMD-5600X");
        body.put("categoryId", 999_999);
        mvc.perform(post("/api/admin/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminListShowsAllStatusesAndFilters() throws Exception {
        product("P-1", "Published", cpus, amd, ProductStatus.PUBLISHED);
        product("D-1", "Draft", cpus, amd, ProductStatus.DRAFT);
        mvc.perform(get("/api/admin/products"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/admin/products").param("status", "DRAFT"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].sku").value("D-1"));
    }

    @Test
    void writeWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(productBody("AMD-5600X"))))
                .andExpect(status().isForbidden());
    }

    // --- kategoriyalar ---

    @Test
    void categoryCrud() throws Exception {
        MvcResult created = mvc.perform(post("/api/admin/categories").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Monitorlar\",\"sortOrder\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sortOrder").value(5))
                .andReturn();
        long id = longAt(created, "$.id");

        mvc.perform(post("/api/admin/categories").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"monitorlar\"}"))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/admin/categories/{id}", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Monitorlar 4K\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Monitorlar 4K"))
                .andExpect(jsonPath("$.sortOrder").value(0));

        mvc.perform(delete("/api/admin/categories/{id}", id).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/admin/categories/{id}", id).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void categoryWithProductsCannotBeDeleted() throws Exception {
        product("P-1", "Ryzen", cpus, null, ProductStatus.DRAFT);
        mvc.perform(delete("/api/admin/categories/{id}", cpus.getId()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Kategoriyada mahsulotlar bor, uni o'chirib bo'lmaydi"));
    }

    // --- brendlar ---

    @Test
    void brandCrud() throws Exception {
        String body = "{\"name\":\"Dell\",\"slug\":\"dell\",\"country\":\"AQSh\",\"founded\":1984,"
                + "\"website\":\"https://www.dell.com\"}";
        long id = longAt(mvc.perform(post("/api/admin/brands").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("dell"))
                .andReturn(), "$.id");

        mvc.perform(post("/api/admin/brands").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Other\",\"slug\":\"dell\"}"))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/admin/brands/{id}", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Dell Technologies\",\"slug\":\"dell\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dell Technologies"));

        mvc.perform(delete("/api/admin/brands/{id}", id).with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    void brandValidation() throws Exception {
        mvc.perform(post("/api/admin/brands").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad\",\"slug\":\"Bad Slug\",\"website\":\"ftp://x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("slug")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("website")));
    }

    @Test
    void brandWithProductsCannotBeDeleted() throws Exception {
        product("P-1", "Ryzen", cpus, amd, ProductStatus.DRAFT);
        mvc.perform(delete("/api/admin/brands/{id}", amd.getId()).with(csrf()))
                .andExpect(status().isConflict());
    }
}
