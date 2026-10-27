package uz.epay.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import uz.epay.entity.Staff;
import uz.epay.entity.StaffRole;
import uz.epay.repository.StaffRepository;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Haqiqiy Tomcat (tasodifiy port) va haqiqiy cookie'lar: MockMvc ko'rsatmaydigan narsalar —
 * JSESSIONID atributlari (HttpOnly, SameSite=Lax), login'dan keyin sessiya ID almashishi, XSRF-TOKEN oqimi.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RealServerSessionTest {
    private static final String PASSWORD = "real-server-pass-1";

    @LocalServerPort
    int port;
    @Autowired
    StaffRepository staffRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private final CookieManager cookies = new CookieManager();
    private final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies)
            .followRedirects(HttpClient.Redirect.NEVER).build();

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private String cookie(String name) {
        return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals(name))
                .map(HttpCookie::getValue).findFirst().orElse(null);
    }

    @Test
    void loginRotatesSessionIdAndCookiesAreHardened() throws Exception {
        String username = "real-" + UUID.randomUUID().toString().substring(0, 8);
        Staff s = new Staff();
        s.setUsername(username);
        s.setFullName("Real");
        s.setRole(StaffRole.OPERATOR);
        s.setPasswordHash(passwordEncoder.encode(PASSWORD));
        staffRepository.save(s);

        // 1) himoyalangan sahifa: login'ga yo'naltiriladi va sessiya ochiladi (so'rov eslab qolinadi)
        HttpResponse<String> page = http.send(HttpRequest.newBuilder(uri("/admin.html")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(page.statusCode()).isEqualTo(302);
        assertThat(page.headers().firstValue("Location").orElseThrow()).endsWith("/login.html?login");
        String before = cookie("JSESSIONID");
        assertThat(before).as("login'dan oldingi sessiya").isNotNull();

        List<String> setCookies = page.headers().allValues("Set-Cookie");
        String sessionCookie = setCookies.stream().filter(c -> c.startsWith("JSESSIONID=")).findFirst().orElseThrow();
        assertThat(sessionCookie).containsIgnoringCase("HttpOnly").containsIgnoringCase("SameSite=Lax");
        // test profili http — Secure faqat prod'da (application-prod.yml)
        assertThat(sessionCookie).doesNotContainIgnoringCase("Secure");

        String xsrf = cookie("XSRF-TOKEN");
        assertThat(xsrf).as("XSRF-TOKEN cookie").isNotNull();
        String xsrfCookie = setCookies.stream().filter(c -> c.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow();
        assertThat(xsrfCookie).as("frontend JS o'qiy olishi kerak").doesNotContainIgnoringCase("HttpOnly");

        // 2) login (X-XSRF-TOKEN sarlavhasi bilan)
        String form = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(PASSWORD, StandardCharsets.UTF_8);
        HttpResponse<String> login = http.send(HttpRequest.newBuilder(uri("/login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "application/json")
                        .header("X-XSRF-TOKEN", xsrf)
                        .POST(HttpRequest.BodyPublishers.ofString(form)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).as(login.body()).isEqualTo(200);
        assertThat(login.body()).contains("\"username\":\"" + username + "\"");

        // session fixation'ga qarshi: yangi ID
        String after = cookie("JSESSIONID");
        assertThat(after).isNotNull().isNotEqualTo(before);

        // 3) yangi sessiya bilan API ishlaydi
        HttpResponse<String> me = http.send(HttpRequest.newBuilder(uri("/api/auth/me"))
                .header("Accept", "application/json").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(me.statusCode()).isEqualTo(200);

        // 4) CSRF tokensiz POST — 403, token bilan — o'tadi (token login'dan keyin yangilangan)
        HttpResponse<String> noToken = http.send(HttpRequest.newBuilder(uri("/api/auth/password"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(noToken.statusCode()).isEqualTo(403);
        HttpResponse<String> wrongToken = http.send(HttpRequest.newBuilder(uri("/api/auth/password"))
                        .header("Content-Type", "application/json")
                        .header("X-XSRF-TOKEN", "boshqa-token")
                        .POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(wrongToken.statusCode()).isEqualTo(403);
        HttpResponse<String> withToken = http.send(HttpRequest.newBuilder(uri("/api/auth/password"))
                        .header("Content-Type", "application/json")
                        .header("X-XSRF-TOKEN", cookie("XSRF-TOKEN"))
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"real-server-pass-2\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(withToken.statusCode()).as(withToken.body()).isEqualTo(204);
    }
}
