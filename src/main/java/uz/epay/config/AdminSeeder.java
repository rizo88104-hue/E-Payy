package uz.epay.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uz.epay.entity.Staff;
import uz.epay.entity.StaffRole;
import uz.epay.repository.StaffRepository;

import java.time.Instant;

@Component
public class AdminSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);
    static final String DEV_PASSWORD = "admin12345";

    private final StaffRepository staff;
    private final PasswordEncoder passwordEncoder;
    private final EpayProperties props;
    private final Environment env;

    public AdminSeeder(StaffRepository staff, PasswordEncoder passwordEncoder, EpayProperties props, Environment env) {
        this.staff = staff;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (staff.count() > 0) {
            return;
        }
        String password = props.admin().password();
        if (password == null || password.isBlank()) {
            if (!env.acceptsProfiles(Profiles.of("dev"))) {
                throw new IllegalStateException("Birinchi admin uchun ADMIN_PASSWORD berilmagan");
            }
            password = DEV_PASSWORD;
            log.warn("ADMIN_PASSWORD berilmagan: dev uchun admin '{}' / '{}' yaratildi", props.admin().username(),
                    DEV_PASSWORD);
        }
        Staff admin = new Staff();
        admin.setUsername(props.admin().username());
        admin.setFullName("Administrator");
        admin.setRole(StaffRole.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setPasswordChangedAt(Instant.now());
        staff.save(admin);
        log.info("Birinchi admin yaratildi: {}", admin.getUsername());
    }
}
