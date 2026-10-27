package uz.epay.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uz.epay.ApiTestBase;
import uz.epay.entity.Brand;
import uz.epay.entity.Category;
import uz.epay.entity.Product;
import uz.epay.entity.ProductStatus;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogApiTest extends ApiTestBase {
    private Category laptops;
    private Category cpus;
    private Brand lenovo;
    private Brand amd;
    private Product draft;
    private Product archived;
    private Product ryzen;

    @BeforeEach
    void seed() {
        laptops = category("Noutbuklar", 2);
        cpus = category("Protsessorlar", 1);
        lenovo = brand("Lenovo", "lenovo");
        amd = brand("AMD", "amd");
        for (int i = 1; i <= 23; i++) {
            product("LAP-" + i, "Laptop " + i, laptops, lenovo, ProductStatus.PUBLISHED);
        }
        ryzen = product("CPU-R5", "AMD Ryzen 5", cpus, amd, ProductStatus.PUBLISHED);
        product("CPU-R7", "AMD Ryzen 7", cpus, amd, ProductStatus.PUBLISHED);
        product("CPU-NOBRAND", "Noma'lum CPU 100%", cpus, null, ProductStatus.PUBLISHED);
        draft = product("DRAFT-1", "Secret Draft Ryzen", cpus, amd, ProductStatus.DRAFT);
        archived = product("OLD-1", "Old Archived Laptop", laptops, lenovo, ProductStatus.ARCHIVED);
    }

    @Test
    void pagination() throws Exception {
        mvc.perform(get("/api/products").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(10)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(26))
                .andExpect(jsonPath("$.totalPages").value(3));
        mvc.perform(get("/api/products").param("size", "10").param("page", "2"))
                .andExpect(jsonPath("$.items", hasSize(6)));
        mvc.perform(get("/api/products").param("size", "10").param("page", "3"))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void defaultPageSizeIs20() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(jsonPath("$.items", hasSize(20)))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void sizeAndPageLimits() throws Exception {
        mvc.perform(get("/api/products").param("size", "100")).andExpect(status().isOk());
        mvc.perform(get("/api/products").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));
        mvc.perform(get("/api/products").param("size", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("page", "-1")).andExpect(status().isBadRequest());
    }

    @Test
    void filterByCategoryAndBrand() throws Exception {
        mvc.perform(get("/api/products").param("categoryId", cpus.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/products").param("brandId", lenovo.getId().toString()).param("size", "100"))
                .andExpect(jsonPath("$.totalElements").value(23))
                .andExpect(jsonPath("$.items[*].brandName", everyItem(is("Lenovo"))));
        mvc.perform(get("/api/products")
                        .param("categoryId", cpus.getId().toString()).param("brandId", amd.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void searchByNameOrSkuCaseInsensitive() throws Exception {
        mvc.perform(get("/api/products").param("q", "RYZEN"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[*].sku", not(hasItem("DRAFT-1"))));
        mvc.perform(get("/api/products").param("q", "lap-7"))
                .andExpect(jsonPath("$.items[*].sku", contains("LAP-7")));
    }

    @Test
    void searchEscapesLikeWildcards() throws Exception {
        mvc.perform(get("/api/products").param("q", "100%"))
                .andExpect(jsonPath("$.items[*].sku", contains("CPU-NOBRAND")));
        mvc.perform(get("/api/products").param("q", "%"))
                .andExpect(jsonPath("$.items[*].sku", contains("CPU-NOBRAND")));
        mvc.perform(get("/api/products").param("q", "_"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void draftAndArchivedAreHidden() throws Exception {
        mvc.perform(get("/api/products").param("size", "100"))
                .andExpect(jsonPath("$.items[*].sku", not(hasItem("DRAFT-1"))))
                .andExpect(jsonPath("$.items[*].sku", not(hasItem("OLD-1"))));
        mvc.perform(get("/api/products/{id}", draft.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/{id}", archived.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/{id}", 999_999)).andExpect(status().isNotFound());
    }

    @Test
    void productDetails() throws Exception {
        mvc.perform(get("/api/products/{id}", ryzen.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("CPU-R5"))
                .andExpect(jsonPath("$.price").value(100.00))
                .andExpect(jsonPath("$.category.name").value("Protsessorlar"))
                .andExpect(jsonPath("$.brand.slug").value("amd"))
                .andExpect(jsonPath("$.images", hasSize(0)));
    }

    @Test
    void categoriesSortedAndBrandsPublic() throws Exception {
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Protsessorlar", "Noutbuklar")));
        mvc.perform(get("/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].slug", contains("amd", "lenovo")));
    }
}
