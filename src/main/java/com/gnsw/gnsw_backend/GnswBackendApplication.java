package com.gnsw.gnsw_backend;

import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.enums.MembershipTier;
import com.gnsw.gnsw_backend.enums.UserStatus;
import com.gnsw.gnsw_backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class GnswBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GnswBackendApplication.class, args);
	}

	@Bean
	CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			var existingAdmin = userRepository.findByEmail("admin@gnsw.ng");
			if (existingAdmin.isPresent()) {
				User admin = existingAdmin.get();
				admin.setPasswordHash(passwordEncoder.encode("Admin@12345"));
				admin.setUsername("admin");
				admin.setRole("ROLE_ADMIN");
				admin.setStatus(UserStatus.ACCEPTED);
				admin.setTier(MembershipTier.FELLOW);
				admin.setEmailVerifiedAt(java.time.LocalDateTime.now());
				userRepository.save(admin);
				System.out.println("✅ Admin password updated: admin / Admin@12345");
			} else {
				User admin = User.builder()
						.email("admin@gnsw.ng")
						.firstName("Super")
						.lastName("Admin")
						.addressLine1("GNSW HQ")
						.city("Lagos")
						.stateProvince("Lagos State")
						.zipPostalCode("100001")
						.country("Nigeria")
						.tier(MembershipTier.FELLOW)
						.status(UserStatus.ACCEPTED)
						.role("ROLE_ADMIN")
						.username("admin")
						.passwordHash(passwordEncoder.encode("Admin@12345"))
						.build();
				userRepository.save(admin);
				System.out.println("✅ Admin user created: admin / Admin@12345");
			}
		};
	}
}