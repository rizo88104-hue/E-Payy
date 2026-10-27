package uz.epay.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.epay.service.StaffService;

import java.io.IOException;

/**
 * Har so'rovda: kirgan xodim bazada bloklangan bo'lsa — sessiya shu yerning o'zida bekor qilinadi.
 * SessionRegistry (bloklash paytida expireNow) bilan birga ikkinchi himoya: registry yo'qolsa ham
 * (qayta ishga tushirish, boshqa nusxa) bloklangan xodim ishlay olmaydi. Bean emas — faqat Security zanjirida.
 */
class StaffStatusFilter extends OncePerRequestFilter {
    private final StaffService staff;
    private final SecurityResponses responses;

    StaffStatusFilter(StaffService staff, SecurityResponses responses) {
        this.staff = staff;
        this.responses = responses;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && staff.isBlocked(auth.getName())) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            responses.deny(req, res, HttpStatus.UNAUTHORIZED, "Hisobingiz bloklangan", "blocked");
            return;
        }
        chain.doFilter(req, res);
    }
}
