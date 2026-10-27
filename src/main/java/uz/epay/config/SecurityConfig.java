package uz.epay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Vaqtinchalik minimal sozlama: ochiq katalog va /api/admin/** uchun faqat "kirgan bo'lsin".
 * Rollar, login va CSRF siyosati 1.3-bosqichda shu yerda to'ldiriladi.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // ochiq katalog — faqat o'qish
                        .requestMatchers(HttpMethod.GET,
                                "/api/categories", "/api/brands", "/api/products", "/api/products/*").permitAll()
                        .requestMatchers("/api/admin/**").authenticated()
                        // holat tekshiruvi va xato sahifasi (aks holda har qanday xato 401 ga aylanadi)
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                        // yuklangan rasmlar va statik frontend
                        .requestMatchers(HttpMethod.GET, "/uploads/**", "/", "/*.html", "/css/**", "/js/**",
                                "/images/**", "/favicon.ico", "/robots.txt").permitAll()
                        .anyRequest().authenticated())
                // TODO(1.3): haqiqiy login bilan almashtiriladi; hozircha dev'da qo'lda sinash uchun
                .httpBasic(Customizer.withDefaults())
                // API: kirmagan bo'lsa 401 (brauzer oynasisiz, WWW-Authenticate yo'q)
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }
}
