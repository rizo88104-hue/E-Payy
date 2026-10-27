package uz.epay.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import uz.epay.ApiTestBase;
import uz.epay.entity.Product;
import uz.epay.entity.ProductStatus;
import uz.epay.service.ImageStorage;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
class ProductImageApiTest extends ApiTestBase {
    private static final String UUID_WEBP =
            "/uploads/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}[.]webp";

    @Autowired
    ImageStorage storage;

    private Product product;

    @BeforeEach
    void seed() {
        product = product("P-1", "Noutbuk", category("Noutbuklar", 0), null, ProductStatus.PUBLISHED);
    }

    private ResultActions upload(String filename, String contentType, byte[] bytes) throws Exception {
        return mvc.perform(multipart("/api/admin/products/{id}/images", product.getId())
                .file(new MockMultipartFile("file", filename, contentType, bytes)).with(csrf()));
    }

    private static byte[] image(String format, int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.BLUE);
        g.fillOval(w / 4, h / 4, w / 2, h / 2);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThat(ImageIO.write(img, format, out)).isTrue();
        return out.toByteArray();
    }

    private Path fileOf(String url) {
        return storage.dir().resolve(url.substring(ImageStorage.URL_PREFIX.length()));
    }

    @Test
    void validPngIsConvertedToWebpAndResized() throws Exception {
        MvcResult res = upload("photo.png", "image/png", image("png", 2400, 1200))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.images", hasSize(1)))
                .andExpect(jsonPath("$.images[0].url").value(matchesPattern(UUID_WEBP)))
                .andExpect(jsonPath("$.images[0].sortOrder").value(0))
                .andReturn();
        String url = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.images[0].url");

        Path file = fileOf(url);
        byte[] saved = Files.readAllBytes(file);
        assertThat(new String(saved, 0, 4)).isEqualTo("RIFF");
        assertThat(new String(saved, 8, 4)).isEqualTo("WEBP");
        BufferedImage back = ImageIO.read(file.toFile());
        assertThat(back.getWidth()).isEqualTo(1200);
        assertThat(back.getHeight()).isEqualTo(600);
    }

    @Test
    void jpegIsAccepted() throws Exception {
        upload("photo.jpg", "image/jpeg", image("jpg", 800, 600))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.images[0].url").value(matchesPattern(UUID_WEBP)));
    }

    /** Tur fayl ichidan aniqlanadi: nomi .png, Content-Type image/png bo'lsa ham, ichi .exe — rad etiladi. */
    @Test
    void exeIsRejected() throws Exception {
        byte[] exe = new byte[1024];
        exe[0] = 'M';
        exe[1] = 'Z';
        upload("virus.exe", "application/octet-stream", exe)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Faqat JPEG, PNG yoki WebP rasm yuklash mumkin"));
        upload("fake.png", "image/png", exe).andExpect(status().isBadRequest());
        assertThat(productRepository.findWithDetailsById(product.getId()).orElseThrow().getImages()).isEmpty();
    }

    @Test
    void sixMegabytesIsRejected() throws Exception {
        byte[] big = Arrays.copyOf(image("png", 100, 100), 6 * 1024 * 1024);
        upload("big.png", "image/png", big)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(ImageStorage.TOO_LARGE));
    }

    @Test
    void brokenImageIsRejected() throws Exception {
        byte[] png = image("png", 100, 100);
        upload("broken.png", "image/png", Arrays.copyOf(png, 40)).andExpect(status().isBadRequest());
    }

    @Test
    void uploadedFileIsServedWithStrictHeaders() throws Exception {
        String url = com.jayway.jsonpath.JsonPath.read(upload("photo.png", "image/png", image("png", 300, 200))
                .andReturn().getResponse().getContentAsString(), "$.images[0].url");
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string("Content-Security-Policy", containsString("sandbox")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", containsString("immutable")));
    }

    /** Papkaga begona fayl tushib qolsa ham (masalan .html) u berilmaydi. */
    @Test
    void onlyUuidWebpNamesAreServed() throws Exception {
        Files.createDirectories(storage.dir());
        Files.writeString(storage.dir().resolve("evil.html"), "<script>alert(1)</script>");
        Files.writeString(storage.dir().resolve("evil.webp"), "x");
        mvc.perform(get("/uploads/evil.html")).andExpect(status().isNotFound());
        mvc.perform(get("/uploads/evil.webp")).andExpect(status().isNotFound());
        mvc.perform(get("/uploads/../application.yml")).andExpect(status().is4xxClientError());
    }

    @Test
    void deleteAndReorderImages() throws Exception {
        upload("a.png", "image/png", image("png", 50, 50)).andExpect(status().isCreated());
        MvcResult two = upload("b.png", "image/png", image("png", 60, 60)).andExpect(status().isCreated()).andReturn();
        long first = longAt(two, "$.images[0].id");
        long second = longAt(two, "$.images[1].id");
        String firstUrl = com.jayway.jsonpath.JsonPath.read(two.getResponse().getContentAsString(), "$.images[0].url");

        mvc.perform(put("/api/admin/products/{id}/images/order", product.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[" + second + "," + first + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images[0].id").value(second))
                .andExpect(jsonPath("$.images[0].sortOrder").value(0))
                .andExpect(jsonPath("$.images[1].id").value(first));

        // ro'yxat to'liq bo'lmasa — 400
        mvc.perform(put("/api/admin/products/{id}/images/order", product.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[" + first + "]}"))
                .andExpect(status().isBadRequest());

        assertThat(fileOf(firstUrl)).exists();
        mvc.perform(delete("/api/admin/products/{id}/images/{imageId}", product.getId(), first).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images", hasSize(1)))
                .andExpect(jsonPath("$.images[0].id").value(second));
        // fayl commit dan keyin o'chiriladi
        assertThat(fileOf(firstUrl)).doesNotExist();
    }

    @Test
    void deletingProductRemovesFiles() throws Exception {
        String url = com.jayway.jsonpath.JsonPath.read(upload("a.png", "image/png", image("png", 50, 50))
                .andReturn().getResponse().getContentAsString(), "$.images[0].url");
        assertThat(fileOf(url)).exists();
        mvc.perform(delete("/api/admin/products/{id}", product.getId()).with(csrf()))
                .andExpect(jsonPath("$.outcome").value("DELETED"));
        assertThat(fileOf(url)).doesNotExist();
    }
}
