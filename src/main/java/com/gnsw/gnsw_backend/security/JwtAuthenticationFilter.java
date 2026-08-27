package com.gnsw.gnsw_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.gnsw.gnsw_backend.entity.AdminUser;
import com.gnsw.gnsw_backend.repository.AdminUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final AdminUserRepository adminUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            String subjectType = jwtTokenProvider.getSubjectTypeFromToken(token);

            UsernamePasswordAuthenticationToken auth = null;

            if ("ADMIN".equals(subjectType)) {
                // Admin-console identity: reload admin + module permissions from DB.
                AdminUser admin = adminUserRepository.findByEmail(username).orElse(null);
                if (admin != null && admin.getPasswordHash() != null
                        && admin.getStatus() == com.gnsw.gnsw_backend.enums.AdminStatus.ACTIVE) {
                    Set<String> authorities = AdminPermissions.authoritiesFor(admin);
                    List<SimpleGrantedAuthority> granted = authorities.stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    auth = new UsernamePasswordAuthenticationToken(admin, null, granted);
                }
            } else {
                // Member / website user identity (unchanged legacy path).
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
            }

            if (auth != null) {
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}