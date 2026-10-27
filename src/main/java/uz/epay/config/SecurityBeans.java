package uz.epay.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/** SecurityConfig va StaffService ikkalasiga kerak bo'lgan bean'lar (aylanma bog'liqlik bo'lmasin). */
@Configuration
@EnableConfigurationProperties(EpayProperties.class)
public class SecurityBeans {

    /** {bcrypt}... — keyinchalik algoritmni almashtirsa, eski xeshlar ham ishlayveradi. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Xodimlarning ochiq sessiyalari (bloklash/parol almashtirishda yopish uchun). Xotirada — bitta nusxa uchun. */
    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /** Sessiya tugashi/ID almashishi haqida SessionRegistry ga xabar beradi (aks holda u eskirgan yozuvlar bilan to'ladi). */
    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
