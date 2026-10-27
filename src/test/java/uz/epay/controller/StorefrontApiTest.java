package uz.epay.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uz.epay.ApiTestBase;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StorefrontApiTest extends ApiTestBase {

    @Test
    void configIsPublicAndComesFromSettings() throws Exception {
        mvc.perform(get("/api/storefront/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usdRate").value(12650.00))
                .andExpect(jsonPath("$.warranty", hasSize(3)))
                .andExpect(jsonPath("$.warranty[0].years").value(1))
                .andExpect(jsonPath("$.warranty[0].percent").value(0))
                .andExpect(jsonPath("$.warranty[1].years").value(3))
                .andExpect(jsonPath("$.warranty[1].percent").value(10))
                .andExpect(jsonPath("$.warranty[2].years").value(5))
                .andExpect(jsonPath("$.warranty[2].percent").value(20))
                .andExpect(jsonPath("$.paymentMethods", hasSize(1)))
                .andExpect(jsonPath("$.paymentMethods[0]").value("CASH"));
    }

    @Test
    void storefrontPagesArePublicWithoutStaffLinksAndWithCsp() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        for (String page : new String[]{"/index.html", "/builder.html", "/brands.html"}) {
            mvc.perform(get(page))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Content-Security-Policy"))
                    .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                    .andExpect(content().string(not(containsString("admin.html"))))
                    .andExpect(content().string(not(containsString("operator.html"))))
                    .andExpect(content().string(not(containsString("login.html"))))
                    .andExpect(content().string(not(containsString("style=\""))))
                    .andExpect(content().string(not(containsString("fonts.googleapis.com"))))
                    .andExpect(content().string(not(containsString("<script>"))))
                    .andExpect(content().string(containsString("<meta name=\"description\"")))
                    .andExpect(content().string(containsString("<title>")));
        }
    }

    @Test
    void staticAssetsArePublic() throws Exception {
        mvc.perform(get("/css/style.css")).andExpect(status().isOk());
        mvc.perform(get("/js/app.js")).andExpect(status().isOk());
        mvc.perform(get("/fonts/onest-latin-400.woff2")).andExpect(status().isOk());
        mvc.perform(get("/fonts/OFL.txt")).andExpect(status().isOk());
        mvc.perform(get("/images/promo/rtx-4070-super.webp")).andExpect(status().isOk());
    }

    @Test
    void robotsHidesStaffPagesAndApi() throws Exception {
        mvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("Disallow: /admin.html")))
                .andExpect(content().string(containsString("Disallow: /operator.html")))
                .andExpect(content().string(containsString("Disallow: /login.html")))
                .andExpect(content().string(containsString("Disallow: /api/")));
    }
}
