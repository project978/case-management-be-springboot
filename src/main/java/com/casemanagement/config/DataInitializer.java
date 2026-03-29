package com.casemanagement.config;

import com.casemanagement.entity.User;
import com.casemanagement.enums.UserRole;
import com.casemanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@casemanagement.com")) {
            User admin = User.builder()
                    .name("Super Admin")
                    .email("admin@casemanagement.com")
                    .phone("9000000000")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(UserRole.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Default admin created → email: admin@casemanagement.com | password: Admin@123");
        }
    }
}
