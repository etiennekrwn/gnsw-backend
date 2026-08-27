package com.gns.gns_backend;

import com.gns.gns_backend.entity.AdminUser;
import com.gns.gns_backend.enums.AdminRole;
import com.gns.gns_backend.enums.AdminStatus;
import com.gns.gns_backend.repository.AdminUserRepository;
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
public class GnsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GnsBackendApplication.class, args);
	}

	@Bean
	CommandLineRunner seedAdmin(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// The one immutable Super Admin lives entirely in the separate
			// admin_users table. Its password is never reset on restart.
			var existing = adminUserRepository.findByEmail("admin@gns.ng");
			if (existing.isPresent()) {
				System.out.println("✅ Super Admin already exists. Skipping.");
				return;
			}
			AdminUser superAdmin = AdminUser.builder()
					.email("admin@gns.ng")
					.displayName("Super Admin")
					.role(AdminRole.SUPER_ADMIN)
					.status(AdminStatus.ACTIVE)
					.passwordHash(passwordEncoder.encode("Admin@12345"))
					.build();
			adminUserRepository.save(superAdmin);
			System.out.println("✅ Super Admin created: admin@gns.ng / Admin@12345 (change at first login).");
		};
	}
}