package uz.epay.exception;

import jakarta.servlet.ServletException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Faqat xato javoblarining shakli tekshiriladi, shuning uchun security filtrlari o'chirilgan. */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void malformedJson() throws Exception {
        mvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("So'rov tanasi (JSON) noto'g'ri yoki o'qib bo'lmaydi"));
    }

    @Test
    void bodyValidationListsFieldsInUzbek() throws Exception {
        mvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"phone\":\"12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Ma'lumotlar noto'g'ri"))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "phone")))
                .andExpect(jsonPath("$.errors[*].message",
                        containsInAnyOrder("bo'sh bo'lmasligi kerak", "uzunligi 9 dan 13 gacha bo'lishi kerak")));
    }

    @Test
    void paramValidation() throws Exception {
        mvc.perform(get("/test/param").param("qty", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("qty"))
                .andExpect(jsonPath("$.errors[0].message").value("1 dan kichik bo'lmasligi kerak"));
    }

    @Test
    void missingAndMistypedParams() throws Exception {
        mvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("'qty' parametri majburiy"));
        mvc.perform(get("/test/param").param("qty", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("'qty' qiymati noto'g'ri"));
    }

    @Test
    void unexpectedErrorHidesDetails() throws Exception {
        String body = mvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.SERVER_ERROR))
                .andExpect(jsonPath("$.code").value(matchesPattern("[0-9a-f-]{36}")))
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain("maxfiy-ichki-xabar")
                .doesNotContain("IllegalStateException")
                .doesNotContain("uz.epay")
                .doesNotContain("trace");
    }

    @Test
    void ownExceptions() throws Exception {
        mvc.perform(get("/test/throw").param("kind", "bad"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("yomon so'rov"));
        mvc.perform(get("/test/throw").param("kind", "notfound"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("topilmadi"));
        mvc.perform(get("/test/throw").param("kind", "forbidden"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail").value("huquq yo'q"));
        mvc.perform(get("/test/throw").param("kind", "conflict"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("to'qnashuv"));
    }

    @Test
    void tooManyRequestsHasRetryAfter() throws Exception {
        mvc.perform(get("/test/throw").param("kind", "limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "2"))   // 1.5 s -> yuqoriga 2
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void optimisticLockIs409WithReason() throws Exception {
        mvc.perform(get("/test/throw").param("kind", "version"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.VERSION_CONFLICT))
                .andExpect(jsonPath("$.reason").value("version"));
    }

    @Test
    void dataIntegrityIs409WithClearText() throws Exception {
        mvc.perform(get("/test/throw").param("kind", "unique"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Bunday yozuv allaqachon mavjud."));
        mvc.perform(get("/test/throw").param("kind", "fk"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Yozuv boshqa ma'lumotlarda ishlatilgan, uni o'chirib yoki o'zgartirib bo'lmaydi."));
    }

    @Test
    void unknownPathIs404() throws Exception {
        mvc.perform(get("/yoq-sahifa.png"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    /** AccessDenied 500 ga aylanmaydi — Spring Security'ga qaytariladi (u 403 beradi). */
    @Test
    void accessDeniedIsNotTurnedInto500() {
        assertThatThrownBy(() -> mvc.perform(get("/test/throw").param("kind", "denied")))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(AccessDeniedException.class);
    }

    record Req(@NotBlank String name, @Size(min = 9, max = 13) String phone) {
    }

    @RestController
    static class TestController {

        @PostMapping("/test/body")
        String body(@Valid @RequestBody Req req) {
            return "ok";
        }

        @GetMapping("/test/param")
        String param(@RequestParam @Min(1) int qty) {
            return "ok";
        }

        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException("maxfiy-ichki-xabar");
        }

        @GetMapping("/test/throw")
        String throwIt(@RequestParam String kind) {
            throw switch (kind) {
                case "bad" -> new BadRequestException("yomon so'rov");
                case "notfound" -> new NotFoundException("topilmadi");
                case "forbidden" -> new ForbiddenException("huquq yo'q");
                case "conflict" -> new ConflictException("to'qnashuv");
                case "limit" -> new TooManyRequestsException("ko'p", Duration.ofMillis(1500));
                case "version" -> new ObjectOptimisticLockingFailureException("Product", 1L);
                case "unique" -> new DataIntegrityViolationException("x",
                        new SQLException("Unique index or primary key violation: PUBLIC.UK_SKU"));
                case "fk" -> new DataIntegrityViolationException("x",
                        new SQLException("violates foreign key constraint \"fk_order_item_product\""));
                case "denied" -> new AccessDeniedException("ruxsat yo'q");
                default -> new IllegalArgumentException(kind);
            };
        }
    }
}
