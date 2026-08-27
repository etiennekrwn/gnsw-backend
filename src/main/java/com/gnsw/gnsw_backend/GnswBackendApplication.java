package com.gnsw.gnsw_backend;

import com.gnsw.gnsw_backend.entity.AdminUser;
import com.gnsw.gnsw_backend.enums.AdminRole;
import com.gnsw.gnsw_backend.enums.AdminStatus;
import com.gnsw.gnsw_backend.repository.AdminUserRepository;
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
	CommandLineRunner seedAdmin(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// The one immutable Super Admin lives entirely in the separate
			// admin_users table. Its password is never reset on restart.
			var existing = adminUserRepository.findByEmail("admin@gnsw.ng");
			if (existing.isPresent()) {
				System.out.println("✅ Super Admin already exists. Skipping.");
				return;
			}
			AdminUser superAdmin = AdminUser.builder()
					.email("admin@gnsw.ng")
					.displayName("Super Admin")
					.role(AdminRole.SUPER_ADMIN)
					.status(AdminStatus.ACTIVE)
					.passwordHash(passwordEncoder.encode("Admin@12345"))
					.build();
			adminUserRepository.save(superAdmin);
			System.out.println("✅ Super Admin created: admin@gnsw.ng / Admin@12345 (change at first login).");
		};
	}
}