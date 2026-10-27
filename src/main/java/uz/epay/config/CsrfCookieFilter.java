package uz.epay.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * CSRF token kechiktirib yuklanadi — so'ralmaguncha XSRF-TOKEN cookie yozilmaydi va frontend (build'siz JS)
 * birinchi POST'ni yubora olmaydi. Bu filtr har javobda tokenni yuklaydi, shunda cookie doim bor.
 * Bean emas — faqat Security zanjirida (CsrfFilter'dan keyin).
 */
class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (req.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
            token.getToken();
        }
        chain.doFilter(req, res);
    }
}
