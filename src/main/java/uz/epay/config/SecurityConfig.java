package uz.epay.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import uz.epay.entity.StaffRole;
import uz.epay.service.StaffService;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Xavfsizlik: form login + server sessiyasi, CSRF (cookie + X-XSRF-TOKEN), rollar TZ 7-bo'lim bo'yicha.
 * Huquq bu yerda (yo'l + metod) va servislarda (masalan, omborchi narxi) — har doim serverda tekshiriladi.
 *
 * <pre>
 *               katalog            ombor     buyurtmalar         pul qaytarish  analitika    xodimlar
 * ADMIN         tahrir             tahrir    hammasi             ha             ha           ha
 * OPERATOR      ko'radi            yo'q      ishlaydi            yo'q           yo'q         yo'q
 * WAREHOUSE     mavjudini tahrir*  tahrir    yo'q                yo'q           ombor        yo'q
 * OWNER         ko'radi            ko'radi   ko'radi             yo'q           ha           yo'q
 * * yaratish/o'chirish yo'q; narx — faqat epay.security.warehouse-can-edit-price=true bo'lsa (ProductService).
 * </pre>
 */
@Configuration
public class SecurityConfig {
    private static final String ADMIN = StaffRole.ADMIN.name();
    private static final String OPERATOR = StaffRole.OPERATOR.name();
    private static final String WAREHOUSE = StaffRole.WAREHOUSE.name();
    private static final String OWNER = StaffRole.OWNER.name();

    /** Inline skript yo'q (barcha JS — alohida fayllarda), CDN yo'q: hammasi o'z domenimizdan. */
    public static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
            + "font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; "
            + "frame-ancestors 'none'";

    /** Payme/Click serverlari CSRF token yubora olmaydi — ular so'rovni o'z imzosi bilan himoyalaydi. */
    static final String[] PAYMENT_CALLBACKS = {"/api/payments/payme", "/api/payments/click/**"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, StaffService staff, RateLimiter limiter,
                                            SecurityResponses responses, SessionRegistry sessionRegistry,
                                            EpayProperties props) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // --- ochiq ---
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/brands", "/api/products",
                                "/api/products/*").permitAll()
                        // xaridor: buyurtma berish va number + telefon bilan kuzatish (1.4)
                        .requestMatchers(HttpMethod.POST, "/api/orders", "/api/orders/track").permitAll()
                        .requestMatchers(HttpMethod.POST, PAYMENT_CALLBACKS).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", SecurityResponses.LOGIN_PAGE,
                                "/css/**", "/js/**", "/images/**", "/uploads/**", "/favicon.ico",
                                "/robots.txt").permitAll()

                        // --- har qanday kirgan xodim ---
                        .requestMatchers("/api/auth/**").authenticated()

                        // --- xodimlar va sozlamalar ---
                        .requestMatchers("/api/admin/staff/**", "/api/admin/settings/**").hasRole(ADMIN)

                        // --- analitika (faqat o'qish) ---
                        .requestMatchers(HttpMethod.GET, "/api/admin/reports/stock/**").hasAnyRole(ADMIN, OWNER, WAREHOUSE)
                        .requestMatchers(HttpMethod.GET, "/api/admin/reports/**").hasAnyRole(ADMIN, OWNER)

                        // --- buyurtmalar ---
                        // REFUNDED belgisi (pul qaytarildi) — faqat admin
                        .requestMatchers("/api/admin/orders/*/refunded").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/admin/orders", "/api/admin/orders/**")
                        .hasAnyRole(ADMIN, OPERATOR, OWNER)
                        .requestMatchers("/api/admin/orders", "/api/admin/orders/**").hasAnyRole(ADMIN, OPERATOR)

                        // --- ombor ---
                        .requestMatchers(HttpMethod.GET, "/api/admin/stock", "/api/admin/stock/**")
                        .hasAnyRole(ADMIN, WAREHOUSE, OWNER)
                        .requestMatchers("/api/admin/stock", "/api/admin/stock/**").hasAnyRole(ADMIN, WAREHOUSE)

                        // --- katalog ---
                        .requestMatchers(HttpMethod.GET, "/api/admin/products", "/api/admin/products/**",
                                "/api/admin/categories/**", "/api/admin/brands/**")
                        .hasAnyRole(ADMIN, OPERATOR, WAREHOUSE, OWNER)
                        // omborchi: mavjud mahsulotni tahrirlash va rasmlari (yaratish/o'chirish — faqat admin)
                        .requestMatchers(HttpMethod.PUT, "/api/admin/products/*").hasAnyRole(ADMIN, WAREHOUSE)
                        .requestMatchers("/api/admin/products/*/images", "/api/admin/products/*/images/**")
                        .hasAnyRole(ADMIN, WAREHOUSE)

                        // --- qolgan admin API (mahsulot yaratish/o'chirish, kategoriya va brend) ---
                        .requestMatchers("/api/admin/**").hasRole(ADMIN)

                        // panel sahifalari va boshqa hamma narsa — kirgan xodimga
                        .anyRequest().authenticated())

                .formLogin(form -> form
                        .loginPage(SecurityResponses.LOGIN_PAGE)
                        .loginProcessingUrl("/login")
                        .successHandler((req, res, auth) -> {
                            // login nomi bo'yicha hisoblagich nolga; IP niki — YO'Q (bitta haqiqiy akkaunt bilan
                            // kirib-chiqib, boshqa akkauntlarni terishni davom ettirib bo'lmasin)
                            limiter.reset(RateLimiter.Limit.LOGIN_USER, StaffService.normalize(auth.getName()));
                            staff.recordLogin(auth.getName());
                            if (responses.wantsJson(req)) {
                                responses.ok(res, staff.me(auth.getName()));
                            } else {
                                res.sendRedirect(req.getContextPath() + "/admin.html");
                            }
                        })
                        .failureHandler((req, res, ex) -> loginFailed(req, res, limiter, responses))
                        .permitAll())
                .addFilterBefore(new LoginRateLimitFilter(limiter, responses), UsernamePasswordAuthenticationFilter.class)

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((req, res, auth) -> {
                            if (responses.wantsJson(req)) {
                                res.setStatus(HttpStatus.NO_CONTENT.value());
                            } else {
                                res.sendRedirect(req.getContextPath() + SecurityResponses.LOGIN_PAGE + "?logout");
                            }
                        }))

                .sessionManagement(session -> session
                        // login'dan keyin yangi sessiya ID (session fixation'ga qarshi)
                        .sessionFixation(fixation -> fixation.changeSessionId())
                        // cheklovsiz, lekin har sessiya SessionRegistry da — bloklash/parol almashtirishda yopiladi
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry)
                        .expiredSessionStrategy(event -> responses.deny(event.getRequest(), event.getResponse(),
                                HttpStatus.UNAUTHORIZED, "Sessiya yopildi. Qayta kiring.", "expired")))
                .addFilterBefore(new StaffStatusFilter(staff, responses), AuthorizationFilter.class)

                // CSRF yoqiq: XSRF-TOKEN cookie, frontend uni X-XSRF-TOKEN sarlavhasida qaytaradi
                .csrf(csrf -> csrf
                        .spa()
                        .ignoringRequestMatchers(PAYMENT_CALLBACKS))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .cors(cors -> cors.configurationSource(corsSource(props)))

                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                        .referrerPolicy(r -> r.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .frameOptions(frame -> frame.deny())
                        // faqat HTTPS so'rovlarda yuboriladi (prod: forward-headers orqali proksidan)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(Duration.ofDays(365).toSeconds())
                                .includeSubDomains(true)))

                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> responses.deny(req, res,
                                HttpStatus.UNAUTHORIZED, "Avval tizimga kiring", "login"))
                        .accessDeniedHandler((req, res, ex) -> responses.deny(req, res, HttpStatus.FORBIDDEN,
                                ex instanceof CsrfException
                                        ? "CSRF token yo'q yoki eskirgan. Sahifani yangilang."
                                        : "Bu amal uchun ruxsat yo'q",
                                "denied")));
        return http.build();
    }

    /** Xato urinish IP va login nomi bo'yicha sanaladi; limitga yetsa — "locked", aks holda umumiy xabar. */
    private static void loginFailed(HttpServletRequest req, HttpServletResponse res, RateLimiter limiter,
                                    SecurityResponses responses) throws IOException {
        String ip = req.getRemoteAddr();
        String user = StaffService.normalize(req.getParameter("username"));
        limiter.hit(RateLimiter.Limit.LOGIN, ip);
        limiter.hit(RateLimiter.Limit.LOGIN_USER, user);
        Duration ipWait = limiter.blockedFor(RateLimiter.Limit.LOGIN, ip);
        Duration userWait = limiter.blockedFor(RateLimiter.Limit.LOGIN_USER, user);
        Duration wait = ipWait.compareTo(userWait) >= 0 ? ipWait : userWait;
        if (!wait.isZero()) {
            responses.locked(req, res, wait);
        } else {
            // bloklangan/yo'q login ham xuddi shu xabarni oladi — akkaunt borligi oshkor bo'lmasin
            responses.deny(req, res, HttpStatus.UNAUTHORIZED, "Login yoki parol noto'g'ri", "error");
        }
    }

    /** Faqat epay.cors.allowed-origin (bitta domen); bo'sh bo'lsa — CORS yo'q. "*" — ishga tushmaydi. */
    static CorsConfigurationSource corsSource(EpayProperties props) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        String origin = props.cors().allowedOrigin();
        if (origin == null || origin.isBlank()) {
            return source;
        }
        if (origin.contains("*")) {
            throw new IllegalStateException("epay.cors.allowed-origin da \"*\" bo'lishi mumkin emas: " + origin);
        }
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin.trim()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "X-Requested-With", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(Duration.ofHours(1));
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
