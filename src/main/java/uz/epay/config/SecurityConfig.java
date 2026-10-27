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

@Configuration
public class SecurityConfig {
    private static final String ADMIN = StaffRole.ADMIN.name();
    private static final String OPERATOR = StaffRole.OPERATOR.name();
    private static final String WAREHOUSE = StaffRole.WAREHOUSE.name();
    private static final String OWNER = StaffRole.OWNER.name();

    public static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
            + "font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; "
            + "frame-ancestors 'none'";

    static final String[] PAYMENT_CALLBACKS = {"/api/payments/payme", "/api/payments/click/**"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, StaffService staff, RateLimiter limiter,
                                            SecurityResponses responses, SessionRegistry sessionRegistry,
                                            EpayProperties props) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/brands", "/api/products",
                                "/api/products/*", "/api/regions").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders", "/api/orders/track").permitAll()
                        .requestMatchers(HttpMethod.POST, PAYMENT_CALLBACKS).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", SecurityResponses.LOGIN_PAGE,
                                "/css/**", "/js/**", "/images/**", "/uploads/**", "/favicon.ico",
                                "/robots.txt").permitAll()

                        .requestMatchers("/api/auth/**").authenticated()

                        .requestMatchers("/api/admin/staff/**", "/api/admin/settings/**").hasRole(ADMIN)

                        .requestMatchers(HttpMethod.GET, "/api/admin/reports/stock/**").hasAnyRole(ADMIN, OWNER, WAREHOUSE)
                        .requestMatchers(HttpMethod.GET, "/api/admin/reports/**").hasAnyRole(ADMIN, OWNER)

                        .requestMatchers("/api/admin/orders/*/refunded").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/admin/orders", "/api/admin/orders/**")
                        .hasAnyRole(ADMIN, OPERATOR, OWNER)
                        .requestMatchers("/api/admin/orders", "/api/admin/orders/**").hasAnyRole(ADMIN, OPERATOR)

                        .requestMatchers(HttpMethod.GET, "/api/admin/stock", "/api/admin/stock/**")
                        .hasAnyRole(ADMIN, WAREHOUSE, OWNER)
                        .requestMatchers("/api/admin/stock", "/api/admin/stock/**").hasAnyRole(ADMIN, WAREHOUSE)

                        .requestMatchers(HttpMethod.GET, "/api/admin/products", "/api/admin/products/**",
                                "/api/admin/categories/**", "/api/admin/brands/**")
                        .hasAnyRole(ADMIN, OPERATOR, WAREHOUSE, OWNER)
                        .requestMatchers(HttpMethod.PUT, "/api/admin/products/*").hasAnyRole(ADMIN, WAREHOUSE)
                        .requestMatchers("/api/admin/products/*/images", "/api/admin/products/*/images/**")
                        .hasAnyRole(ADMIN, WAREHOUSE)

                        .requestMatchers("/api/admin/**").hasRole(ADMIN)

                        .anyRequest().authenticated())

                .formLogin(form -> form
                        .loginPage(SecurityResponses.LOGIN_PAGE)
                        .loginProcessingUrl("/login")
                        .successHandler((req, res, auth) -> {
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
                        .sessionFixation(fixation -> fixation.changeSessionId())
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry)
                        .expiredSessionStrategy(event -> responses.deny(event.getRequest(), event.getResponse(),
                                HttpStatus.UNAUTHORIZED, "Sessiya yopildi. Qayta kiring.", "expired")))
                .addFilterBefore(new StaffStatusFilter(staff, responses), AuthorizationFilter.class)

                .csrf(csrf -> csrf
                        .spa()
                        .ignoringRequestMatchers(PAYMENT_CALLBACKS))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .cors(cors -> cors.configurationSource(corsSource(props)))

                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                        .referrerPolicy(r -> r.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .frameOptions(frame -> frame.deny())
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
            responses.deny(req, res, HttpStatus.UNAUTHORIZED, "Login yoki parol noto'g'ri", "error");
        }
    }

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
