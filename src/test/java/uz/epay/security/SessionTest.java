package uz.epay.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import uz.epay.ApiTestBase;
import uz.epay.entity.Staff;
import uz.epay.entity.StaffRole;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionTest extends ApiTestBase {
    private static final String PASSWORD = "session-pass-1";
    private static final String ADMIN_PASSWORD = "session-admin-1";

    private String operator;
    private Staff operatorStaff;
    private String admin;

    @BeforeEach
    void seed() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        operator = "op-" + suffix;
        admin = "adm-" + suffix;
        operatorStaff = staff(operator, StaffRole.OPERATOR, PASSWORD);
        staff(admin, StaffRole.ADMIN, ADMIN_PASSWORD);
    }

    private MockHttpSession login(String username, String password) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).with(fromIp("10.50.0." + username.length()))
                        .accept(MediaType.APPLICATION_JSON).param("username", username).param("password", password))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    void blockedStaffSessionIsClosedOnNextRequest() throws Exception {
        MockHttpSession opSession = login(operator, PASSWORD);
        MockHttpSession adminSession = login(admin, ADMIN_PASSWORD);
        mvc.perform(get("/api/admin/products").session(opSession)).andExpect(status().isOk());

        mvc.perform(patch("/api/admin/staff/{id}/active", operatorStaff.getId()).session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get("/api/admin/products").session(opSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.reason").value("expired"));
        mvc.perform(get("/api/admin/staff").session(adminSession)).andExpect(status().isOk());
    }

    @Test
    void blockedInDatabaseIsAlsoClosed() throws Exception {
        MockHttpSession opSession = login(operator, PASSWORD);
        operatorStaff.setActive(false);
        staffRepository.save(operatorStaff);

        mvc.perform(get("/api/admin/products").session(opSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.reason").value("blocked"));
        mvc.perform(get("/api/admin/products").session(opSession)).andExpect(status().isUnauthorized());
    }

    @Test
    void ownPasswordChangeClosesOtherSessions() throws Exception {
        MockHttpSession phone = login(operator, PASSWORD);
        MockHttpSession laptop = login(operator, PASSWORD);

        mvc.perform(post("/api/auth/password").session(laptop).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"brand-new-pass-1\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").session(laptop)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(phone)).andExpect(status().isUnauthorized());
        login(operator, "brand-new-pass-1");
    }

    @Test
    void ownPasswordChangeRequiresCurrentPassword() throws Exception {
        MockHttpSession s = login(operator, PASSWORD);
        mvc.perform(post("/api/auth/password").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"brand-new-pass-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Joriy parol noto'g'ri"));
    }

    @Test
    void adminPasswordResetClosesStaffSessions() throws Exception {
        MockHttpSession opSession = login(operator, PASSWORD);
        MockHttpSession adminSession = login(admin, ADMIN_PASSWORD);

        mvc.perform(put("/api/admin/staff/{id}/password", operatorStaff.getId()).session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"reset-by-admin-1\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/auth/me").session(opSession)).andExpect(status().isUnauthorized());
        login(operator, "reset-by-admin-1");
    }
}
