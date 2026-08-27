package com.gns.gns_backend.config;

import com.gns.gns_backend.security.JwtAuthenticationFilter;
import com.gns.gns_backend.security.RestAccessDeniedHandler;
import com.gns.gns_backend.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication needed
                        .requestMatchers("/api/v1/public/**").permitAll()
                        // Admin user-management is reserved for full admins.
                        .requestMatchers("/api/v1/admin/users-admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        // Module-scoped checks for MANAGER accounts. SUPER_ADMIN
                        // and ADMIN implicitly hold every MODULE_* authority.
                        .requestMatchers("/api/v1/admin/applications/**", "/api/v1/admin/members/**", "/api/v1/admin/tiers/**").hasAuthority("MODULE_MEMBERS")
                        // Every admin-console identity (incl. MANAGER) reaches the base.
                        .requestMatchers("/api/v1/admin/**").hasAuthority("ADMIN_IDENTITY")
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // Missing/invalid/expired token -> 401 (triggers frontend logout).
                        .authenticationEntryPoint(authenticationEntryPoint)
                        // Authenticated but lacking a specific permission -> 403.
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}