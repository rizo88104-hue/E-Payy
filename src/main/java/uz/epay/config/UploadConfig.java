package uz.epay.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import uz.epay.service.ImageStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

@Configuration
public class UploadConfig implements WebMvcConfigurer {
    static final String UPLOADS_CSP = "default-src 'none'; img-src 'self'; style-src 'unsafe-inline'; sandbox";

    private final Path dir;

    public UploadConfig(@Value("${epay.upload-dir}") String dir) {
        this.dir = ImageStorage.resolveDir(dir);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = dir.toUri().toString();
        registry.addResourceHandler(ImageStorage.URL_PREFIX + "**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .resourceChain(false)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        return ImageStorage.FILE_NAME.matcher(resourcePath).matches()
                                ? super.getResource(resourcePath, location) : null;
                    }
                });
    }

    @Bean
    FilterRegistrationBean<OncePerRequestFilter> uploadHeadersFilter() {
        FilterRegistrationBean<OncePerRequestFilter> reg = new FilterRegistrationBean<>(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws ServletException, IOException {
                res.setHeader("Content-Security-Policy", UPLOADS_CSP);
                res.setHeader("X-Content-Type-Options", "nosniff");
                chain.doFilter(req, res);
            }
        });
        reg.addUrlPatterns(ImageStorage.URL_PREFIX + "*");
        return reg;
    }
}
