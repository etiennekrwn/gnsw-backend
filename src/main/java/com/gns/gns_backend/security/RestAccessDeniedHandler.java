package com.gns.gns_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gns.gns_backend.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a JSON {@code 403 Forbidden} when an authenticated admin is denied a
 * specific resource (e.g. a MANAGER attempting a full-admin-only action). This
 * is distinct from {@link RestAuthenticationEntryPoint} which returns
 * {@code 401} for missing/invalid/expired credentials.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .message("You do not have permission to perform this action.")
                .build();
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}