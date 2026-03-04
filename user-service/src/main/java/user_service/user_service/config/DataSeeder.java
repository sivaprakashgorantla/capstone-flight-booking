package user_service.user_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import user_service.user_service.model.Role;
import user_service.user_service.model.User;
import user_service.user_service.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("========================================================");
        log.info("  User Service — Seeding default users...");
        log.info("========================================================");

        seedUser("admin", "admin@flight.com", "admin@123", "Admin",   "User",  Role.ADMIN);
        seedUser("user",  "user@flight.com",  "user@123",  "Default", "User",  Role.USER);

        log.info("========================================================");
        log.info("  Default credentials (change in production!)");
        log.info("  ADMIN → username: admin  password: admin@123");
        log.info("  USER  → username: user   password: user@123");
        log.info("========================================================");
    }

    private void seedUser(String username, String email, String rawPassword,
                          String firstName, String lastName, Role role) {
        if (userRepository.existsByUsername(username)) {
            log.info("  [SKIP] '{}' already exists.", username);
            return;
        }
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .active(true)
                .build();

        userRepository.save(user);
        log.info("  [OK]   '{}' created with role {}.", username, role);
    }
}
