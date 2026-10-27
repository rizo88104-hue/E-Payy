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
import uz.epay.entity.Order;
import uz.epay.entity.PaymentStatus;
import uz.epay.entity.Product;
import uz.epay.entity.ProductStatus;
import uz.epay.entity.Staff;
import uz.epay.entity.StaffRole;
import uz.epay.service.OrderEvent;
import uz.epay.service.OrderService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Xodimlar: buyurtmalar ro'yxati, holatlar, operator cheklovlari. Operatorlar bazada bor (op1, op2) —
 * OrderService "kim" ekanini login nomi bo'yicha topadi.
 */
@RecordApplicationEvents
class AdminOrderApiTest extends ApiTestBase {
    @Autowired
    ApplicationEvents events;
    @Autowired
    OrderService orderService;

    Product product;
    Staff op1;
    Staff op2;

    @BeforeEach
    void seed() {
        product = product("P-1", "Mahsulot", category("Kategoriya", 0), null, ProductStatus.PUBLISHED);
        op1 = staff("op1", StaffRole.OPERATOR, "operator-pass-1");
        op2 = staff("op2", StaffRole.OPERATOR, "operator-pass-2");
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor as(String name, StaffRole role) {
        return user(name).roles(role.name());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return as("boss", StaffRole.ADMIN);
    }

    private ResultActions changeStatus(Long id, String status, org.springframework.test.web.servlet.request.RequestPostProcessor who)
            throws Exception {
        return mvc.perform(patch("/api/admin/orders/{id}/status", id).with(who).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
    }

    private void assignTo(Long orderId, Staff s) {
        Order o = orderRepository.findById(orderId).orElseThrow();
        o.setAssignedOperator(s);
        orderRepository.save(o);
    }

    // --- ro'yxat va filtrlar ---

    @Test
    void listWithFiltersForAdmin() throws Exception {
        String a = placeOrder(product, 1, "+998901111111");
        String b = placeOrder(product, 1, "+998902222222");
        changeStatus(orderId(b), "CONFIRMED", admin()).andExpect(status().isOk());

        mvc.perform(get("/api/admin/orders").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].number").value(b));
        mvc.perform(get("/api/admin/orders").with(admin()).param("status", "NEW"))
                .andExpect(jsonPath("$.items[*].number", containsInAnyOrder(a)));
        mvc.perform(get("/api/admin/orders").with(admin()).param("payStatus", "PAID"))
                .andExpect(jsonPath("$.totalElements").value(0));
        // qidiruv: raqam, telefon raqamlari, ism
        mvc.perform(get("/api/admin/orders").with(admin()).param("q", a.toLowerCase()))
                .andExpect(jsonPath("$.items[*].number", containsInAnyOrder(a)));
        mvc.perform(get("/api/admin/orders").with(admin()).param("q", "90 222"))
                .andExpect(jsonPath("$.items[*].number", containsInAnyOrder(b)));
        mvc.perform(get("/api/admin/orders").with(admin()).param("q", "valiyev"))
                .andExpect(jsonPath("$.totalElements").value(2));
        // sana: bugun (Toshkent) — ikkalasi; kecha gacha — hech biri
        LocalDate today = LocalDate.now(OrderService.TASHKENT);
        mvc.perform(get("/api/admin/orders").with(admin()).param("from", today.toString()).param("to", today.toString()))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/admin/orders").with(admin()).param("to", today.minusDays(1).toString()))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/admin/orders").with(admin()).param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detailsForStaff() throws Exception {
        Long id = orderId(placeOrder(product, 2, "+998901234567"));
        mvc.perform(get("/api/admin/orders/{id}", id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Ali Valiyev"))
                .andExpect(jsonPath("$.address").value("Chilonzor 1-uy"))
                .andExpect(jsonPath("$.region").value("TOSHKENT_SHAHRI"))
                .andExpect(jsonPath("$.usdRate").value(12650.00))
                .andExpect(jsonPath("$.items[0].sku").value("P-1"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
        // egasi faqat ko'radi
        mvc.perform(get("/api/admin/orders/{id}", id).with(as("owner", StaffRole.OWNER))).andExpect(status().isOk());
        changeStatus(id, "CONFIRMED", as("owner", StaffRole.OWNER)).andExpect(status().isForbidden());
    }

    // --- operator cheklovlari ---

    @Test
    void operatorSeesOnlyUnassignedAndOwn() throws Exception {
        Long free = orderId(placeOrder(product, 1, "+998901111111"));
        Long mine = orderId(placeOrder(product, 1, "+998902222222"));
        Long others = orderId(placeOrder(product, 1, "+998903333333"));
        assignTo(mine, op1);
        assignTo(others, op2);

        mvc.perform(get("/api/admin/orders").with(as("op1", StaffRole.OPERATOR)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[*].id", containsInAnyOrder(free.intValue(), mine.intValue())));
        mvc.perform(get("/api/admin/orders/{id}", others).with(as("op1", StaffRole.OPERATOR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Bu buyurtma boshqa operatorga biriktirilgan"));
        changeStatus(others, "CONFIRMED", as("op1", StaffRole.OPERATOR)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/orders/{id}/note", others).with(as("op1", StaffRole.OPERATOR)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"x\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/orders/{id}/take", others).with(as("op1", StaffRole.OPERATOR)).with(csrf()))
                .andExpect(status().isForbidden());
        // admin hammasini ko'radi
        mvc.perform(get("/api/admin/orders/{id}", others).with(admin())).andExpect(status().isOk());
    }

    @Test
    void takeAndAutoClaim() throws Exception {
        Long a = orderId(placeOrder(product, 1, "+998901111111"));
        Long b = orderId(placeOrder(product, 1, "+998902222222"));

        mvc.perform(post("/api/admin/orders/{id}/take", a).with(as("op1", StaffRole.OPERATOR)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operatorId").value(op1.getId()));
        // op2 endi uni ko'rmaydi
        mvc.perform(get("/api/admin/orders/{id}", a).with(as("op2", StaffRole.OPERATOR)))
                .andExpect(status().isForbidden());

        // biriktirilmagan buyurtmani tasdiqlasa — avtomatik unga biriktiriladi
        changeStatus(b, "CONFIRMED", as("op2", StaffRole.OPERATOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operatorId").value(op2.getId()));
        mvc.perform(patch("/api/admin/orders/{id}/note", b).with(as("op2", StaffRole.OPERATOR)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"  Ertaga 10:00 da  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operatorNote").value("Ertaga 10:00 da"));
    }

    @Test
    void onlyAdminAssignsToOthers() throws Exception {
        Long id = orderId(placeOrder(product, 1, "+998901111111"));
        mvc.perform(put("/api/admin/orders/{id}/operator", id).with(as("op1", StaffRole.OPERATOR)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"operatorId\":" + op2.getId() + "}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/orders/{id}/operator", id).with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"operatorId\":" + op2.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operatorId").value(op2.getId()))
                .andExpect(jsonPath("$.operatorName").value("Test op2"));
        // omborchiga biriktirib bo'lmaydi
        Staff w = staff("wh1", StaffRole.WAREHOUSE, "warehouse-pass");
        mvc.perform(put("/api/admin/orders/{id}/operator", id).with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"operatorId\":" + w.getId() + "}"))
                .andExpect(status().isBadRequest());
    }

    // --- holat o'tishlari ---

    @Test
    void validTransitionsAndEvents() throws Exception {
        Long id = orderId(placeOrder(product, 1, "+998901111111"));
        changeStatus(id, "CONFIRMED", admin()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        changeStatus(id, "DELIVERED", admin()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELIVERED"));
        assertThat(events.stream(OrderEvent.class)).extracting(OrderEvent::type)
                .containsExactly(OrderEvent.Type.CREATED, OrderEvent.Type.CONFIRMED);
    }

    @Test
    void invalidTransitionsAre400() throws Exception {
        Long id = orderId(placeOrder(product, 1, "+998901111111"));
        changeStatus(id, "DELIVERED", admin())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Holatni o'zgartirib bo'lmaydi: NEW → DELIVERED"));
        changeStatus(id, "NEW", admin()).andExpect(status().isOk());   // o'zgarishsiz — xato emas

        changeStatus(id, "CANCELLED", admin()).andExpect(status().isOk());
        // CANCELLED — yakuniy
        changeStatus(id, "CONFIRMED", admin()).andExpect(status().isBadRequest());
        changeStatus(id, "DELIVERED", admin()).andExpect(status().isBadRequest());

        Long done = orderId(placeOrder(product, 1, "+998902222222"));
        changeStatus(done, "CONFIRMED", admin());
        changeStatus(done, "DELIVERED", admin());
        // DELIVERED — yakuniy
        changeStatus(done, "CANCELLED", admin()).andExpect(status().isBadRequest());
        changeStatus(done, "BOSHQA", admin()).andExpect(status().isBadRequest());
    }

    /** To'langan buyurtma bekor qilinsa — REFUND_PENDING; "pul qaytarildi" (REFUNDED) — faqat admin. */
    @Test
    void cancellingPaidOrderNeedsRefund() throws Exception {
        Long id = orderId(placeOrder(product, 1, "+998901111111"));
        orderService.markPaid(id);
        orderService.markPaid(id);   // takroriy callback — hech narsa qilmaydi
        assertThat(orderRepository.findById(id).orElseThrow().getPaidAt()).isNotNull();

        changeStatus(id, "CANCELLED", as("op1", StaffRole.OPERATOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.payStatus").value("REFUND_PENDING"));

        mvc.perform(patch("/api/admin/orders/{id}/refunded", id).with(as("op1", StaffRole.OPERATOR)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/orders/{id}/refunded", id).with(admin()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payStatus").value("REFUNDED"));
        // ikkinchi marta — o'tish yo'q
        mvc.perform(patch("/api/admin/orders/{id}/refunded", id).with(admin()).with(csrf()))
                .andExpect(status().isBadRequest());

        assertThat(events.stream(OrderEvent.class)).extracting(OrderEvent::type)
                .containsExactly(OrderEvent.Type.CREATED, OrderEvent.Type.PAID, OrderEvent.Type.CANCELLED);
    }

    @Test
    void unpaidCancelStaysUnpaidAndCancelledCannotBePaid() throws Exception {
        Long id = orderId(placeOrder(product, 1, "+998901111111"));
        changeStatus(id, "CANCELLED", admin()).andExpect(jsonPath("$.payStatus").value("UNPAID"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> orderService.markPaid(id))
                .isInstanceOf(uz.epay.exception.ConflictException.class);
        assertThat(orderRepository.findById(id).orElseThrow().getPayStatus()).isEqualTo(PaymentStatus.UNPAID);
        // REFUND_PENDING bo'lmagan buyurtmani REFUNDED qilib bo'lmaydi
        mvc.perform(patch("/api/admin/orders/{id}/refunded", id).with(admin()).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    /** Buyurtmalar bilan ishlagan xodim o'chirilmaydi — tarix saqlanadi (bloklash mumkin). */
    @Test
    void staffWithOrdersCannotBeDeleted() throws Exception {
        Long id = orderId(placeOrder(product, 1, "+998901111111"));
        assignTo(id, op1);
        mvc.perform(delete("/api/admin/staff/{id}", op1.getId()).with(admin()).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/admin/staff/{id}", op2.getId()).with(admin()).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "WAREHOUSE")
    void warehouseHasNoOrders() throws Exception {
        mvc.perform(get("/api/admin/orders")).andExpect(status().isForbidden());
    }
}
