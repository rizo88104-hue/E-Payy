package uz.epay.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uz.epay.ApiTestBase;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

class RoleMatrixTest extends ApiTestBase {
    static final String ANON = "ANON";
    static final List<String> ROLES = List.of(ANON, "ADMIN", "OPERATOR", "WAREHOUSE", "OWNER");

    static final String ADMIN = "ADMIN";
    static final String OPERATOR = "OPERATOR";
    static final String WAREHOUSE = "WAREHOUSE";
    static final String OWNER = "OWNER";
    static final String[] STAFF = {ADMIN, OPERATOR, WAREHOUSE, OWNER};
    static final String[] EVERYONE = {ANON, ADMIN, OPERATOR, WAREHOUSE, OWNER};

    record Case(HttpMethod method, String path, Set<String> allowed) {
        Case(HttpMethod method, String path, String... allowed) {
            this(method, path, Set.of(allowed));
        }
    }

    static final List<Case> MATRIX = List.of(
            new Case(HttpMethod.GET, "/api/products", EVERYONE),
            new Case(HttpMethod.GET, "/api/categories", EVERYONE),
            new Case(HttpMethod.GET, "/api/regions", EVERYONE),
            new Case(HttpMethod.GET, "/api/storefront/config", EVERYONE),
            new Case(HttpMethod.POST, "/api/orders", EVERYONE),
            new Case(HttpMethod.POST, "/api/orders/track", EVERYONE),

            new Case(HttpMethod.GET, "/api/admin/products", STAFF),
            new Case(HttpMethod.GET, "/api/admin/products/1", STAFF),
            new Case(HttpMethod.POST, "/api/admin/products", ADMIN),
            new Case(HttpMethod.PUT, "/api/admin/products/1", ADMIN, WAREHOUSE),
            new Case(HttpMethod.DELETE, "/api/admin/products/1", ADMIN),
            new Case(HttpMethod.POST, "/api/admin/products/1/images", ADMIN, WAREHOUSE),
            new Case(HttpMethod.PUT, "/api/admin/products/1/images/order", ADMIN, WAREHOUSE),
            new Case(HttpMethod.DELETE, "/api/admin/products/1/images/2", ADMIN, WAREHOUSE),
            new Case(HttpMethod.POST, "/api/admin/categories", ADMIN),
            new Case(HttpMethod.PUT, "/api/admin/categories/1", ADMIN),
            new Case(HttpMethod.DELETE, "/api/admin/categories/1", ADMIN),
            new Case(HttpMethod.POST, "/api/admin/brands", ADMIN),
            new Case(HttpMethod.DELETE, "/api/admin/brands/1", ADMIN),

            new Case(HttpMethod.GET, "/api/admin/stock", ADMIN, WAREHOUSE, OWNER),
            new Case(HttpMethod.PUT, "/api/admin/stock/1", ADMIN, WAREHOUSE),

            new Case(HttpMethod.GET, "/api/admin/orders", ADMIN, OPERATOR, OWNER),
            new Case(HttpMethod.GET, "/api/admin/orders/1", ADMIN, OPERATOR, OWNER),
            new Case(HttpMethod.POST, "/api/admin/orders/1/take", ADMIN, OPERATOR),
            new Case(HttpMethod.PATCH, "/api/admin/orders/1/status", ADMIN, OPERATOR),
            new Case(HttpMethod.PATCH, "/api/admin/orders/1/note", ADMIN, OPERATOR),
            new Case(HttpMethod.PUT, "/api/admin/orders/1/operator", ADMIN, OPERATOR),
            new Case(HttpMethod.PATCH, "/api/admin/orders/1/refunded", ADMIN),

            new Case(HttpMethod.GET, "/api/admin/reports/sales", ADMIN, OWNER),
            new Case(HttpMethod.GET, "/api/admin/reports/stock", ADMIN, OWNER, WAREHOUSE),

            new Case(HttpMethod.GET, "/api/admin/staff", ADMIN),
            new Case(HttpMethod.POST, "/api/admin/staff", ADMIN),
            new Case(HttpMethod.PATCH, "/api/admin/staff/1/active", ADMIN),
            new Case(HttpMethod.GET, "/api/admin/settings", ADMIN),

            new Case(HttpMethod.GET, "/api/auth/me", STAFF)
    );

    @org.junit.jupiter.api.BeforeEach
    void staffForEveryRole() {
        if (hash == null) {
            hash = passwordEncoder.encode("matrix-password");
        }
        for (String role : STAFF) {
            uz.epay.entity.Staff s = new uz.epay.entity.Staff();
            s.setUsername("matrix-" + role.toLowerCase());
            s.setFullName("Matrix " + role);
            s.setRole(uz.epay.entity.StaffRole.valueOf(role));
            s.setPasswordHash(hash);
            staffRepository.save(s);
        }
    }

    private static String hash;

    static Stream<Arguments> matrix() {
        return MATRIX.stream().flatMap(c -> ROLES.stream()
                .map(role -> Arguments.of(role, c.method(), c.path(), c.allowed().contains(role))));
    }

    @ParameterizedTest(name = "{0} {1} {2} -> {3}")
    @MethodSource("matrix")
    void roleMatrix(String role, HttpMethod method, String path, boolean allowed) throws Exception {
        MockHttpServletRequestBuilder req = request(method, path).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}");
        if (!role.equals(ANON)) {
            req = req.with(user("matrix-" + role.toLowerCase()).roles(role));
        }
        int status = mvc.perform(req).andReturn().getResponse().getStatus();

        if (allowed) {
            assertThat(status).as("%s %s %s ruxsat etilishi kerak", role, method, path).isNotIn(401, 403);
        } else {
            assertThat(status).as("%s %s %s rad etilishi kerak", role, method, path)
                    .isEqualTo(role.equals(ANON) ? 401 : 403);
        }
    }

    @org.junit.jupiter.api.Test
    void matrixIsConsistent() {
        for (String role : ROLES) {
            assertThat(MATRIX.stream().anyMatch(c -> c.allowed().contains(role))).as(role + " ruxsat").isTrue();
            if (!role.equals(ADMIN)) {
                assertThat(MATRIX.stream().anyMatch(c -> !c.allowed().contains(role))).as(role + " rad").isTrue();
            }
        }
        assertThat(MATRIX).allMatch(c -> c.allowed().contains(ADMIN));
        assertThat(Arrays.asList(STAFF)).doesNotContain(ANON);
    }
}
