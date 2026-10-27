package uz.epay.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uz.epay.dto.StaffCreateRequest;
import uz.epay.dto.StaffResponse;
import uz.epay.entity.Staff;
import uz.epay.entity.StaffRole;
import uz.epay.exception.BadRequestException;
import uz.epay.exception.ConflictException;
import uz.epay.exception.NotFoundException;
import uz.epay.repository.StaffRepository;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Xodimlar: login (UserDetailsService), boshqaruv va sessiyalar.
 * Bloklash, o'chirish va parol almashtirishda sessiyalar SessionRegistry orqali yopiladi —
 * registry xotirada, bitta ilova nusxasi uchun yetarli (bir nechta nusxa bo'lsa — Spring Session JDBC).
 */
@Service
public class StaffService implements UserDetailsService {
    private final StaffRepository staff;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessions;

    public StaffService(StaffRepository staff, PasswordEncoder passwordEncoder, SessionRegistry sessions) {
        this.staff = staff;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
    }

    public static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    // --- login ---

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        Staff s = staff.findByUsername(normalize(username))
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return User.withUsername(s.getUsername())
                .password(s.getPasswordHash())
                .roles(s.getRole().name())
                .disabled(!s.isActive())
                .build();
    }

    @Transactional
    public void recordLogin(String username) {
        staff.findByUsername(normalize(username)).ifPresent(s -> s.setLastLoginAt(Instant.now()));
    }

    /** Bazada bor va bloklangan. (Yo'q xodimning sessiyasi o'chirishda registry orqali yopiladi.) */
    @Transactional(readOnly = true)
    public boolean isBlocked(String username) {
        return staff.findByUsername(normalize(username)).map(s -> !s.isActive()).orElse(false);
    }

    @Transactional(readOnly = true)
    public StaffResponse me(String username) {
        return StaffResponse.from(find(username));
    }

    /** O'z parolini almashtiradi: joriy parol tekshiriladi; boshqa sessiyalari yopiladi, joriysi qoladi. */
    @Transactional
    public void changeOwnPassword(String username, String currentPassword, String newPassword) {
        Staff s = find(username);
        if (!passwordEncoder.matches(currentPassword, s.getPasswordHash())) {
            throw new BadRequestException("Joriy parol noto'g'ri");
        }
        setPassword(s, newPassword);
        expireSessions(s.getUsername(), currentSessionId());
    }

    // --- admin ---

    @Transactional(readOnly = true)
    public List<StaffResponse> list() {
        return staff.findAllByOrderByRoleAscFullNameAsc().stream().map(StaffResponse::from).toList();
    }

    @Transactional
    public StaffResponse create(StaffCreateRequest req) {
        String username = normalize(req.username());
        if (staff.existsByUsername(username)) {
            throw new ConflictException("Bu login band: " + username);
        }
        Staff s = new Staff();
        s.setUsername(username);
        s.setFullName(req.fullName().trim());
        s.setPhone(req.phone() == null || req.phone().isBlank() ? null : PhoneNumbers.normalize(req.phone()));
        s.setRole(req.role());
        setPassword(s, req.password());
        return StaffResponse.from(staff.save(s));
    }

    /** Bloklash: barcha ochiq sessiyalari darhol yopiladi. O'zini va oxirgi faol adminni bloklab bo'lmaydi. */
    @Transactional
    public StaffResponse setActive(Long id, boolean active) {
        Staff s = get(id);
        if (!active) {
            requireNotSelf(s, "O'zingizni bloklay olmaysiz");
            requireNotLastAdmin(s);
            expireSessions(s.getUsername(), null);
        }
        s.setActive(active);
        return StaffResponse.from(s);
    }

    /** Admin xodim parolini almashtiradi: xodimning boshqa sessiyalari yopiladi (o'zini almashtirsa — joriysi qoladi). */
    @Transactional
    public StaffResponse resetPassword(Long id, String password) {
        Staff s = get(id);
        setPassword(s, password);
        expireSessions(s.getUsername(), currentSessionId());
        return StaffResponse.from(s);
    }

    @Transactional
    public void delete(Long id) {
        Staff s = get(id);
        requireNotSelf(s, "O'zingizni o'chira olmaysiz");
        requireNotLastAdmin(s);
        // TODO(1.4): buyurtmalar bilan ishlagan xodim o'chirilmaydi — faqat bloklanadi
        staff.delete(s);
        expireSessions(s.getUsername(), null);
    }

    // --- yordamchilar ---

    private void setPassword(Staff s, String password) {
        s.setPasswordHash(passwordEncoder.encode(password));
        s.setPasswordChangedAt(Instant.now());
    }

    private void requireNotSelf(Staff s, String message) {
        if (s.getUsername().equals(normalize(CurrentUser.username()))) {
            throw new BadRequestException(message);
        }
    }

    /** Tizim adminsiz qolmasin. */
    private void requireNotLastAdmin(Staff s) {
        if (s.getRole() == StaffRole.ADMIN && s.isActive() && staff.countByRoleAndActiveTrue(StaffRole.ADMIN) <= 1) {
            throw new ConflictException("Oxirgi faol administratorni bloklab yoki o'chirib bo'lmaydi");
        }
    }

    /** Xodimning sessiyalarini yopadi (keepSessionId dan tashqari): keyingi so'rovda u chiqarib yuboriladi. */
    private void expireSessions(String username, String keepSessionId) {
        for (Object principal : sessions.getAllPrincipals()) {
            if (principal instanceof UserDetails u && u.getUsername().equals(username)) {
                for (SessionInformation info : sessions.getAllSessions(principal, false)) {
                    if (!info.getSessionId().equals(keepSessionId)) {
                        info.expireNow();
                    }
                }
            }
        }
    }

    /** Joriy so'rov sessiyasi (yangisini yaratmaydi: getSessionId() yaratib yuborardi). */
    private static String currentSessionId() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        HttpSession session = attrs.getRequest().getSession(false);
        return session == null ? null : session.getId();
    }

    private Staff get(Long id) {
        return staff.findById(id).orElseThrow(() -> new NotFoundException("Xodim topilmadi: " + id));
    }

    private Staff find(String username) {
        return staff.findByUsername(normalize(username))
                .orElseThrow(() -> new NotFoundException("Xodim topilmadi: " + username));
    }
}
