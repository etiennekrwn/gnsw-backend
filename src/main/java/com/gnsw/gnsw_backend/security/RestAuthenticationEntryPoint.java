package com.gnsw.gnsw_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnsw.gnsw_backend.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a structured JSON {@code 401 Unauthorized} when a request reaches a
 * protected endpoint without valid authentication (missing, invalid, or
 * expired token). Previously Spring Security fell back to the default
 * {@code Http403ForbiddenEntryPoint}, which made stale admin tokens surface as
 * {@code 403} instead of {@code 401}, so the admin frontend (which only logs
 * the user out on {@code 401}) never realised the session had expired.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .message("Authentication required. Please log in again.")
                .build();
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}