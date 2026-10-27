package uz.epay.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import uz.epay.ApiTestBase;
import uz.epay.entity.Staff;
import uz.epay.entity.StaffRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(username = "boss", roles = "ADMIN")
class StaffApiTest extends ApiTestBase {
    private Staff boss;

    @BeforeEach
    void seed() {
        boss = staff("boss", StaffRole.ADMIN, "boss-password-1");
    }

    @Test
    void createAndList() throws Exception {
        mvc.perform(post("/api/admin/staff").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Ombor.1\",\"password\":\"warehouse-pass\",\"fullName\":\"Omborchi\","
                                + "\"phone\":\"90 123 45 67\",\"role\":\"WAREHOUSE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ombor.1"))
                .andExpect(jsonPath("$.phone").value("+998901234567"))
                .andExpect(jsonPath("$.role").value("WAREHOUSE"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertThat(passwordEncoder.matches("warehouse-pass",
                staffRepository.findByUsername("ombor.1").orElseThrow().getPasswordHash())).isTrue();

        mvc.perform(get("/api/admin/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItem("ombor.1")));
    }

    @Test
    void duplicateUsernameIs409() throws Exception {
        mvc.perform(post("/api/admin/staff").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"BOSS\",\"password\":\"some-password\",\"fullName\":\"X\",\"role\":\"OWNER\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void validation() throws Exception {
        mvc.perform(post("/api/admin/staff").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a b\",\"password\":\"short\",\"fullName\":\"\",\"role\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("username")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("password")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("fullName")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("role")));
    }

    @Test
    void blockAndUnblock() throws Exception {
        Staff op = staff("op-1", StaffRole.OPERATOR, "operator-pass");
        mvc.perform(patch("/api/admin/staff/{id}/active", op.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(patch("/api/admin/staff/{id}/active", op.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void cannotBlockOrDeleteSelf() throws Exception {
        mvc.perform(patch("/api/admin/staff/{id}/active", boss.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O'zingizni bloklay olmaysiz"));
        mvc.perform(delete("/api/admin/staff/{id}", boss.getId()).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O'zingizni o'chira olmaysiz"));
    }

    @Test
    @WithMockUser(username = "ghost", roles = "ADMIN")
    void cannotBlockOrDeleteLastActiveAdmin() throws Exception {
        jdbc.update("update staff set active = false where role = 'ADMIN' and username <> 'boss'");
        assertThat(staffRepository.countByRoleAndActiveTrue(StaffRole.ADMIN)).isEqualTo(1);

        mvc.perform(patch("/api/admin/staff/{id}/active", boss.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Oxirgi faol administratorni bloklab yoki o'chirib bo'lmaydi"));
        mvc.perform(delete("/api/admin/staff/{id}", boss.getId()).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void resetPasswordAndDelete() throws Exception {
        Staff op = staff("op-2", StaffRole.OPERATOR, "operator-pass");
        mvc.perform(put("/api/admin/staff/{id}/password", op.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"new-operator-pass\"}"))
                .andExpect(status().isOk());
        assertThat(passwordEncoder.matches("new-operator-pass",
                staffRepository.findById(op.getId()).orElseThrow().getPasswordHash())).isTrue();

        mvc.perform(put("/api/admin/staff/{id}/password", op.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(delete("/api/admin/staff/{id}", op.getId()).with(csrf())).andExpect(status().isNoContent());
        assertThat(staffRepository.findById(op.getId())).isEmpty();
    }

    @Test
    void me() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("boss"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
