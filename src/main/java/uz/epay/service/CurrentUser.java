package uz.epay.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uz.epay.entity.StaffRole;

/** Joriy so'rovdagi xodim (SecurityContext dan). Servislar huquqni serverda shu orqali tekshiradi. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static boolean hasRole(StaffRole role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + role.name()).equals(a.getAuthority()));
    }

    /** Login nomi yoki null (kirmagan bo'lsa). */
    public static String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }
}
