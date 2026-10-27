package uz.epay.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import uz.epay.ApiTestBase;
import uz.epay.entity.StaffRole;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Form login, IP va login nomi bo'yicha limit. Har test o'z IP va login nomi bilan — limitlar aralashmasin. */
class LoginTest extends ApiTestBase {
    private static final String PASSWORD = "correct-horse-1";

    private String username;
    private String ip;

    @BeforeEach
    void seed() {
        username = "op-" + UUID.randomUUID().toString().substring(0, 8);
        ip = "10.20." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250);
        staff(username, StaffRole.OPERATOR, PASSWORD);
    }

    private ResultActions login(String user, String password, String fromIp) throws Exception {
        return mvc.perform(post("/login").with(csrf()).with(fromIp(fromIp))
                .accept(MediaType.APPLICATION_JSON)
                .param("username", user).param("password", password));
    }

    @Test
    void successReturnsCurrentStaffAndRecordsLogin() throws Exception {
        login(username, PASSWORD, ip)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("OPERATOR"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertThat(staffRepository.findByUsername(username).orElseThrow().getLastLoginAt()).isNotNull();
    }

    @Test
    void usernameIsCaseInsensitive() throws Exception {
        login(username.toUpperCase(), PASSWORD, ip).andExpect(status().isOk());
    }


    @Test
    void wrongPasswordAndUnknownUserGetSameMessage() throws Exception {
        login(username, "wrong-password", ip)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Login yoki parol noto'g'ri"))
                .andExpect(jsonPath("$.reason").value("error"));
        login("no-such-user", "whatever-123", ip)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Login yoki parol noto'g'ri"));
    }

    /** Bloklangan xodim to'g'ri parol bilan ham kira olmaydi — va xabar boshqalarnikidan farq qilmaydi. */
    @Test
    void blockedStaffCannotLogin() throws Exception {
        var s = staffRepository.findByUsername(username).orElseThrow();
        s.setActive(false);
        staffRepository.save(s);
        login(username, PASSWORD, ip)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Login yoki parol noto'g'ri"));
    }

    /** 10 xatodan keyin shu IP dan to'g'ri parol ham o'tmaydi; boshqa IP dan — o'tadi. */
    @Test
    void ipIsLockedAfterTenFailuresEvenWithCorrectPassword() throws Exception {
        for (int i = 1; i <= 9; i++) {
            login(username, "wrong-" + i, ip).andExpect(status().isUnauthorized());
        }
        login(username, "wrong-10", ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.reason").value("locked"));

        login(username, PASSWORD, ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.reason").value("locked"))
                .andExpect(header().exists("Retry-After"));
        // blokda ham login nomi sanalmaydi — parol umuman tekshirilmadi
        login(username, PASSWORD, "10.99.99.1").andExpect(status().isOk());
    }

    /** Bitta login nomiga turli IP lardan 30 xato — keyin istalgan IP dan ham (to'g'ri parol bilan) blok. */
    @Test
    void usernameIsLockedAfterManyFailuresFromDifferentIps() throws Exception {
        for (int i = 1; i <= 30; i++) {
            login(username, "wrong", "10.30." + (i / 250) + "." + (i % 250)).andReturn();
        }
        login(username, PASSWORD, "10.31.0.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.reason").value("locked"));
        // boshqa xodim shu IP dan bemalol kiradi
        staff("other-" + username, StaffRole.OPERATOR, PASSWORD);
        login("other-" + username, PASSWORD, "10.31.0.1").andExpect(status().isOk());
    }

    @Test
    void htmlFormGetsRedirects() throws Exception {
        mvc.perform(post("/login").with(csrf()).with(fromIp(ip)).param("username", username).param("password", "x"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login.html?error"));
        mvc.perform(post("/login").with(csrf()).with(fromIp(ip)).param("username", username).param("password", PASSWORD))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin.html"));
    }

    @Test
    void loginWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/login").with(fromIp(ip)).accept(MediaType.APPLICATION_JSON)
                        .param("username", username).param("password", PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void logout() throws Exception {
        MockHttpSession session = (MockHttpSession) login(username, PASSWORD, ip).andReturn().getRequest().getSession();
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(session).with(csrf()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }
}
