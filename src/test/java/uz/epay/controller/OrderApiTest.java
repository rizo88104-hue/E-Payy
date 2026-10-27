package uz.epay.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;
import uz.epay.ApiTestBase;
import uz.epay.entity.Category;
import uz.epay.entity.Order;
import uz.epay.entity.OrderItem;
import uz.epay.entity.Product;
import uz.epay.entity.ProductStatus;
import uz.epay.service.OrderEvent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RecordApplicationEvents
class OrderApiTest extends ApiTestBase {
    @Autowired
    ApplicationEvents events;

    Category category;
    Product laptop;

    @BeforeEach
    void seed() {
        category = category("Noutbuklar", 0);
        laptop = product("LAP-1", "Noutbuk", category, null, ProductStatus.PUBLISHED);
        laptop.setPrice(new BigDecimal("1299.99"));
        laptop.setMxikCode("08471001001000000");
        laptop.setPackageCode("1501275");
        laptop = productRepository.save(laptop);
    }

    private Map<String, Object> body(List<Map<String, Object>> items) {
        return new java.util.HashMap<>(Map.of("customerName", "Ali Valiyev", "phone", "+998 90 123 45 67",
                "address", "Toshkent, Chilonzor 1-uy", "region", "TOSHKENT_SHAHRI", "items", items));
    }

    private static Map<String, Object> item(Product p, int qty) {
        return Map.of("productId", p.getId(), "quantity", qty);
    }

    private static Map<String, Object> item(Product p, int qty, int years) {
        return Map.of("productId", p.getId(), "quantity", qty, "warrantyYears", years);
    }

    private ResultActions order(Map<String, Object> body) throws Exception {
        return order(body, freshIp());
    }

    private ResultActions order(Map<String, Object> body, String ip) throws Exception {
        return mvc.perform(post("/api/orders").with(csrf()).with(fromIp(ip))
                .contentType(MediaType.APPLICATION_JSON).content(toJson(body)));
    }

    @Test
    void createFreezesPriceRateAndAmount() throws Exception {
        order(body(List.of(item(laptop, 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(matchesPattern("EP-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.payStatus").value("UNPAID"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.totalUsd").value(1299.99))
                .andExpect(jsonPath("$.amountUzs").value(16_444_874))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.customerName").doesNotExist())
                .andExpect(jsonPath("$.operatorNote").doesNotExist());

        Order saved = orderRepository.findAll().getFirst();
        assertThat(saved.getUsdRate()).isEqualByComparingTo("12650.00");
        assertThat(saved.getPhone()).isEqualTo("+998901234567");
        assertThat(saved.getExpiresAt()).isAfter(saved.getCreatedAt());

        laptop.setPrice(new BigDecimal("1.00"));
        productRepository.save(laptop);
        Order again = orderRepository.findWithDetailsById(saved.getId()).orElseThrow();
        assertThat(again.getTotalUsd()).isEqualByComparingTo("1299.99");
        assertThat(again.getAmountUzs()).isEqualTo(16_444_874L);
        OrderItem line = again.getItems().getFirst();
        assertThat(line.getUnitPriceUsd()).isEqualByComparingTo("1299.99");
        assertThat(line.getProductName()).isEqualTo("Noutbuk");
        assertThat(line.getSku()).isEqualTo("LAP-1");
        assertThat(line.getMxikCode()).isEqualTo("08471001001000000");
        assertThat(line.getPackageCode()).isEqualTo("1501275");
        assertThat(line.getWarrantyYears()).isEqualTo(1);

        assertThat(events.stream(OrderEvent.class)).extracting(OrderEvent::type).containsExactly(OrderEvent.Type.CREATED);
    }

    @Test
    void orderNumbersAreSequential() throws Exception {
        String a = placeOrder(laptop, 1, "+998901111111");
        String b = placeOrder(laptop, 1, "+998902222222");
        long na = Long.parseLong(a.substring(a.lastIndexOf('-') + 1));
        long nb = Long.parseLong(b.substring(b.lastIndexOf('-') + 1));
        assertThat(nb).isEqualTo(na + 1);
    }

    @Test
    void warrantySurchargeIsRoundedHalfUp() throws Exception {
        Product p = product("RAM-1", "RAM", category, null, ProductStatus.PUBLISHED);
        p.setPrice(new BigDecimal("99.95"));
        productRepository.save(p);
        Product q = product("RAM-2", "RAM 2", category, null, ProductStatus.PUBLISHED);
        q.setPrice(new BigDecimal("99.95"));
        productRepository.save(q);

        order(body(List.of(item(p, 2, 3), item(q, 1, 5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalUsd").value(339.84))
                .andExpect(jsonPath("$.amountUzs").value(4_298_976));
    }

    @Test
    void unknownWarrantyIs400() throws Exception {
        order(body(List.of(item(laptop, 1, 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Kafolat muddati noto'g'ri: 2 yil. Mavjud: [1, 3, 5] yil"));
    }

    @Test
    void duplicateLinesAreMerged() throws Exception {
        order(body(List.of(item(laptop, 4), item(laptop, 5)))).andExpect(status().isCreated());
        Order o = orderRepository.findWithDetailsById(orderRepository.findAll().getFirst().getId()).orElseThrow();
        assertThat(o.getItems()).hasSize(1);
        assertThat(o.getItems().getFirst().getQuantity()).isEqualTo(9);

        order(body(List.of(item(laptop, 6), item(laptop, 5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Bitta mahsulotdan ko'pi bilan 10 ta buyurtma qilish mumkin"));
    }

    @Test
    void validation() throws Exception {
        Map<String, Object> b = body(List.of(item(laptop, 1)));
        b.put("customerName", "A");
        b.put("address", "1234");
        b.remove("region");
        order(b).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field",
                        org.hamcrest.Matchers.containsInAnyOrder("customerName", "address", "region")));

        Map<String, Object> badPhone = body(List.of(item(laptop, 1)));
        badPhone.put("phone", "12345");
        order(badPhone).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Telefon raqam noto'g'ri. Masalan: +998 90 123 45 67"));

        Map<String, Object> badRegion = body(List.of(item(laptop, 1)));
        badRegion.put("region", "MOSKVA");
        order(badRegion).andExpect(status().isBadRequest());

        order(body(List.of())).andExpect(status().isBadRequest());
        order(body(List.of(item(laptop, 11)))).andExpect(status().isBadRequest());
        order(body(List.of(item(laptop, 0)))).andExpect(status().isBadRequest());
    }

    @Test
    void atMostTwentyLines() throws Exception {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            items.add(item(product("P-" + i, "P" + i, category, null, ProductStatus.PUBLISHED), 1));
        }
        order(body(items)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("items"));
        order(body(items.subList(0, 20))).andExpect(status().isCreated());
    }

    @Test
    void onlyPublishedProducts() throws Exception {
        Product draft = product("D-1", "Draft", category, null, ProductStatus.DRAFT);
        Product archived = product("A-1", "Archived", category, null, ProductStatus.ARCHIVED);
        order(body(List.of(item(draft, 1)))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Mahsulot sotuvda yo'q: " + draft.getId()));
        order(body(List.of(item(archived, 1)))).andExpect(status().isBadRequest());
        Map<String, Object> missing = body(List.of(Map.of("productId", 999_999, "quantity", 1)));
        order(missing).andExpect(status().isBadRequest());
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void tooManyPendingOrdersPerPhoneIs409() throws Exception {
        for (int i = 0; i < 3; i++) {
            placeOrder(laptop, 1, "+998 93 555 44 33");
        }
        order(new java.util.HashMap<>(Map.of("customerName", "Ali", "phone", "935554433",
                "address", "Toshkent 1-uy", "region", "ANDIJON", "items", List.of(item(laptop, 1)))))
                .andExpect(status().isConflict());
        placeOrder(laptop, 1, "+998 93 555 44 34");
    }

    @Test
    void sixthOrderFromSameIpIs429() throws Exception {
        String ip = freshIp();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> b = body(List.of(item(laptop, 1)));
            b.put("phone", "+99890700000" + i);
            order(b, ip).andExpect(status().isCreated());
        }
        Map<String, Object> sixth = body(List.of(item(laptop, 1)));
        sixth.put("phone", "+998907000009");
        order(sixth, ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    private ResultActions track(String number, String phone) throws Exception {
        return mvc.perform(post("/api/orders/track").with(csrf()).with(fromIp(freshIp()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("number", number, "phone", phone))));
    }

    @Test
    void trackWithMatchingPhone() throws Exception {
        String number = placeOrder(laptop, 2, "+998901234567");
        track(number, "90 123 45 67")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(number))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.phoneMasked").value("+998 90 *** ** 67"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].lineTotalUsd").value(2599.98))
                .andExpect(jsonPath("$.customerName").doesNotExist())
                .andExpect(jsonPath("$.address").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist());
        track(number.toLowerCase(), "+998901234567").andExpect(status().isOk());
    }

    @Test
    void trackMismatchIsAlwaysSame404() throws Exception {
        String number = placeOrder(laptop, 1, "+998901234567");
        String wrongPhone = track(number, "+998901234568").andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        String noOrder = track("EP-2000-999999", "+998901234567").andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        String badFormat = track(number, "abc").andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        assertThat(wrongPhone).isEqualTo(noOrder).isEqualTo(badFormat).contains("Buyurtma topilmadi");
    }

    @Test
    void staffEndpointsNeedLogin() throws Exception {
        String number = placeOrder(laptop, 1, "+998901234567");
        Long id = orderId(number);
        mvc.perform(get("/api/orders/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/orders/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void regionsInThreeLanguages() throws Exception {
        mvc.perform(get("/api/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(14)))
                .andExpect(jsonPath("$[0].code").value("TOSHKENT_SHAHRI"))
                .andExpect(jsonPath("$[0].uz").value("Toshkent shahri"))
                .andExpect(jsonPath("$[0].ru").value("г. Ташкент"))
                .andExpect(jsonPath("$[0].en").value("Tashkent city"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void productUsedInOrderIsArchivedNotDeleted() throws Exception {
        placeOrder(laptop, 1, "+998901234567");
        mvc.perform(delete("/api/admin/products/{id}", laptop.getId()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ARCHIVED"));
        assertThat(productRepository.findById(laptop.getId()).orElseThrow().getStatus())
                .isEqualTo(ProductStatus.ARCHIVED);
        mvc.perform(get("/api/products/{id}", laptop.getId())).andExpect(status().isNotFound());
        assertThat(orderRepository.count()).isEqualTo(1);

        Product unused = product("U-1", "Unused", category, null, ProductStatus.PUBLISHED);
        mvc.perform(delete("/api/admin/products/{id}", unused.getId()).with(csrf()))
                .andExpect(jsonPath("$.outcome").value("DELETED"));
    }
}
