package uz.epay.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SecurityResponses {
    public static final String LOGIN_PAGE = "/login.html";
    static final RequestMatcher API = PathPatternRequestMatcher.pathPattern("/api/**");

    private final ObjectMapper json;

    public SecurityResponses(ObjectMapper json) {
        this.json = json;
    }

    public boolean wantsJson(HttpServletRequest req) {
        if (API.matches(req)) {
            return true;
        }
        String accept = req.getHeader(HttpHeaders.ACCEPT);
        try {
            return accept != null && MediaType.parseMediaTypes(accept).stream()
                    .anyMatch(m -> !m.isWildcardType() && m.isCompatibleWith(MediaType.APPLICATION_JSON));
        } catch (InvalidMediaTypeException e) {
            return false;
        }
    }

    public void problem(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String detail,
                        Map<String, Object> extra) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("instance", req.getRequestURI());
        body.putAll(extra);
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getWriter(), body);
    }

    public void problem(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String detail)
            throws IOException {
        problem(req, res, status, detail, Map.of());
    }

    public void ok(HttpServletResponse res, Object body) throws IOException {
        res.setStatus(HttpStatus.OK.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getWriter(), body);
    }

    public void deny(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String detail, String reason)
            throws IOException {
        if (wantsJson(req)) {
            problem(req, res, status, detail, Map.of("reason", reason));
        } else {
            res.sendRedirect(req.getContextPath() + LOGIN_PAGE + "?" + reason);
        }
    }

    public void locked(HttpServletRequest req, HttpServletResponse res, Duration retryAfter) throws IOException {
        res.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1, (retryAfter.toMillis() + 999) / 1000)));
        deny(req, res, HttpStatus.TOO_MANY_REQUESTS,
                "Juda ko'p muvaffaqiyatsiz urinish. Birozdan keyin qayta urinib ko'ring.", "locked");
    }
}
