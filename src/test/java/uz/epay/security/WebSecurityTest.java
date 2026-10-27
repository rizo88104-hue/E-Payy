package uz.epay.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import uz.epay.ApiTestBase;
import uz.epay.config.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CSRF, CORS, xavfsizlik sarlavhalari va rad javoblari (API — JSON, sahifa — login'ga). */
class WebSecurityTest extends ApiTestBase {
    private static final String ALLOWED = "https://shop.epay.test";

    // --- CSRF ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void postWithoutCsrfIs403() throws Exception {
        mvc.perform(post("/api/admin/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("CSRF token yo'q yoki eskirgan. Sahifani yangilang."));
    }

    /** Payme/Click CSRF token yubora olmaydi — ular uchun CSRF o'chiq (o'z imzosi bor). */
    @Test
    void paymentCallbacksPassWithoutCsrf() throws Exception {
        for (String path : new String[]{"/api/payments/payme", "/api/payments/click/prepare",
                "/api/payments/click/complete"}) {
            int status = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andReturn().getResponse().getStatus();
            assertThat(status).as(path).isNotIn(401, 403);
        }
    }

    // --- rad javoblari ---

    @Test
    void apiWithoutLoginIs401Json() throws Exception {
        mvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Avval tizimga kiring"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void apiForbiddenIs403Json() throws Exception {
        mvc.perform(get("/api/admin/staff"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Bu amal uchun ruxsat yo'q"));
    }

    @Test
    void pageWithoutLoginRedirectsToLogin() throws Exception {
        mvc.perform(get("/admin.html"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login.html?login"));
    }

    // --- CORS ---

    @Test
    void corsForeignOriginIsRejected() throws Exception {
        mvc.perform(options("/api/products")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        mvc.perform(get("/api/products").header(HttpHeaders.ORIGIN, "https://evil.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsAllowedOrigin() throws Exception {
        mvc.perform(options("/api/products")
                        .header(HttpHeaders.ORIGIN, ALLOWED)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    // --- sarlavhalar ---

    @Test
    void securityHeaders() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(header().string("Content-Security-Policy", SecurityConfig.CSP))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                // HSTS faqat HTTPS so'rovda
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
        mvc.perform(get("/api/products").secure(true))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"));
        assertThat(SecurityConfig.CSP).contains("script-src 'self'").doesNotContain("unsafe-inline")
                .doesNotContain("http").contains("frame-ancestors 'none'");
    }
}
