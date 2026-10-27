package uz.epay.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.epay.service.StaffService;

import java.io.IOException;
import java.time.Duration;

class LoginRateLimitFilter extends OncePerRequestFilter {
    static final RequestMatcher LOGIN_POST = PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/login");

    private final RateLimiter limiter;
    private final SecurityResponses responses;

    LoginRateLimitFilter(RateLimiter limiter, SecurityResponses responses) {
        this.limiter = limiter;
        this.responses = responses;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (LOGIN_POST.matches(req)) {
            Duration ip = limiter.blockedFor(RateLimiter.Limit.LOGIN, req.getRemoteAddr());
            Duration user = limiter.blockedFor(RateLimiter.Limit.LOGIN_USER,
                    StaffService.normalize(req.getParameter("username")));
            Duration wait = ip.compareTo(user) >= 0 ? ip : user;
            if (!wait.isZero()) {
                responses.locked(req, res, wait);
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
