package uz.epay.service;

import com.luciad.imageio.webp.WebPWriteParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uz.epay.exception.BadRequestException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Yuklangan mahsulot rasmlari: turini fayl ichidan (magic bytes) aniqlaydi, kichraytiradi, WebP qilib
 * epay.upload-dir ga UUID nomi bilan yozadi. Foydalanuvchi bergan nom, kengaytma va Content-Type ishlatilmaydi.
 */
@Service
public class ImageStorage {
    private static final Logger log = LoggerFactory.getLogger(ImageStorage.class);

    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final String TOO_LARGE = "Fayl hajmi 5 MB dan oshmasin";
    /** Eng uzun tomon, px. */
    static final int MAX_SIDE = 1200;
    /** "Siqilgan bomba"ga qarshi: 5 MB PNG ham 50 000×50 000 deb e'lon qilishi mumkin. */
    static final long MAX_PIXELS = 50_000_000L;
    static final float QUALITY = 0.82f;
    public static final String URL_PREFIX = "/uploads/";
    /** Bizning fayllarimiz: faqat shu ko'rinishdagi nomlar beriladi va o'chiriladi. */
    public static final Pattern FILE_NAME =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}[.]webp");

    private final Path dir;

    public ImageStorage(@Value("${epay.upload-dir}") String dir) {
        this.dir = resolveDir(dir);
    }

    /** epay.upload-dir → mutlaq yo'l (UploadConfig ham shu bilan hisoblaydi). */
    public static Path resolveDir(String dir) {
        return Path.of(dir).toAbsolutePath().normalize();
    }

    public Path dir() {
        return dir;
    }

    /** Tekshiradi, WebP ga o'tkazib saqlaydi va "/uploads/{uuid}.webp" qaytaradi. */
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Rasm fayli tanlanmagan");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException(TOO_LARGE);
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Faylni o'qib bo'lmadi");
        }
        BufferedImage image = resize(decode(bytes, detect(bytes)));
        String name = UUID.randomUUID() + ".webp";
        try {
            Files.createDirectories(dir);
            // avval vaqtinchalik faylga: yarim yozilgan rasm hech qachon /uploads/ da ko'rinmaydi
            Path tmp = Files.createTempFile(dir, "up-", ".tmp");
            try {
                writeWebp(image, tmp);
                Files.move(tmp, dir.resolve(name), StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Rasmni saqlab bo'lmadi", e);
        }
        return URL_PREFIX + name;
    }

    /** Faqat bizning "/uploads/{uuid}.webp" fayllarimiz o'chiriladi; boshqa qiymat e'tiborsiz qoldiriladi. */
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }
        String name = url.substring(URL_PREFIX.length());
        if (!FILE_NAME.matcher(name).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(dir.resolve(name));
        } catch (IOException e) {
            log.warn("Rasm faylini o'chirib bo'lmadi: {}", name, e);
        }
    }

    enum Kind { JPEG, PNG, WEBP }

    static Kind detect(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Kind.JPEG;
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return Kind.PNG;
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Kind.WEBP;
        }
        throw new BadRequestException("Faqat JPEG, PNG yoki WebP rasm yuklash mumkin");
    }

    private static BufferedImage decode(byte[] bytes, Kind kind) {
        // WebP faqat TwelveMonkeys (sof Java) bilan o'qiladi: begona fayl native libwebp ga berilmaydi
        ImageReader reader = kind == Kind.WEBP
                ? reader("webp", r -> r.getOriginatingProvider().getClass().getName().startsWith("com.twelvemonkeys."))
                : reader(kind == Kind.JPEG ? "jpeg" : "png", r -> true);
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            reader.setInput(in, true, true);
            long w = reader.getWidth(0);
            long h = reader.getHeight(0);
            // o'lcham sarlavhadan o'qiladi — piksellar xotiraga yuklanishidan OLDIN
            if (w <= 0 || h <= 0 || w * h > MAX_PIXELS) {
                throw new BadRequestException("Rasm o'lchami juda katta (50 megapikselgacha)");
            }
            ImageReadParam param = reader.getDefaultReadParam();
            // juda katta rasmni o'qishdayoq siyraklashtiradi (xotira tejaladi), aniq o'lcham resize() da
            int step = (int) Math.max(1, Math.max(w, h) / (MAX_SIDE * 2L));
            param.setSourceSubsampling(step, step, 0, 0);
            return reader.read(0, param);
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new BadRequestException("Rasm faylini o'qib bo'lmadi: fayl buzilgan yoki rasm emas");
        } finally {
            reader.dispose();
        }
    }

    private static ImageReader reader(String format, Predicate<ImageReader> accept) {
        Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName(format);
        while (it.hasNext()) {
            ImageReader r = it.next();
            if (accept.test(r)) {
                return r;
            }
            r.dispose();
        }
        throw new IllegalStateException("ImageIO o'quvchisi topilmadi: " + format);
    }

    /** Eng uzun tomoni MAX_SIDE gacha; bosqichma-bosqich (har safar ko'pi bilan 2 marta) — sifatliroq. */
    static BufferedImage resize(BufferedImage src) {
        int type = src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        int w = src.getWidth();
        int h = src.getHeight();
        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(w, h));
        int tw = Math.max(1, (int) Math.round(w * scale));
        int th = Math.max(1, (int) Math.round(h * scale));
        BufferedImage cur = src;
        do {
            w = Math.max(tw, w / 2);
            h = Math.max(th, h / 2);
            BufferedImage next = new BufferedImage(w, h, type);
            Graphics2D g = next.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(cur, 0, 0, w, h, null);
            g.dispose();
            cur = next;
        } while (w != tw || h != th);
        return cur;
    }

    private static void writeWebp(BufferedImage image, Path target) throws IOException {
        Iterator<ImageWriter> it = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!it.hasNext()) {
            throw new IllegalStateException("WebP yozuvchisi topilmadi");
        }
        ImageWriter writer = it.next();
        WebPWriteParam param = new WebPWriteParam(writer.getLocale());
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionType(param.getCompressionTypes()[WebPWriteParam.LOSSY_COMPRESSION]);
        param.setCompressionQuality(QUALITY);
        try (ImageOutputStream out = ImageIO.createImageOutputStream(target.toFile())) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}
