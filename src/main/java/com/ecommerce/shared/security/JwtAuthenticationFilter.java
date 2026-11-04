package com.ecommerce.shared.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String MDC_ACTOR_KEY = "actor";
  private static final String MDC_CORRELATION_ID_KEY = "correlationId";

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String existingCorrelationId = MDC.get(MDC_CORRELATION_ID_KEY);
    boolean correlationIdSetLocally = false;
    if (existingCorrelationId == null || existingCorrelationId.isBlank()) {
      String correlationId = request.getHeader(CORRELATION_ID_HEADER);
      if (correlationId == null || correlationId.isBlank()) {
        correlationId = UUID.randomUUID().toString();
      }
      MDC.put(MDC_CORRELATION_ID_KEY, correlationId);
      response.setHeader(CORRELATION_ID_HEADER, correlationId);
      correlationIdSetLocally = true;
    }

    boolean actorSet = false;

    try {
      String authHeader = request.getHeader(AUTHORIZATION_HEADER);
      if (authHeader != null && authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        Optional<Claims> validClaims = jwtService.parseValidClaims(token);
        if (validClaims.isPresent()) {
          Authentication authentication = jwtService.buildAuthentication(validClaims.get());
          SecurityContext context = SecurityContextHolder.createEmptyContext();
          context.setAuthentication(authentication);
          SecurityContextHolder.setContext(context);

          if (authentication.getName() != null && !authentication.getName().isBlank()) {
            MDC.put(MDC_ACTOR_KEY, authentication.getName());
            actorSet = true;
          }
        }
      }

      filterChain.doFilter(request, response);
    } finally {
      if (actorSet) {
        MDC.remove(MDC_ACTOR_KEY);
      }
      if (correlationIdSetLocally) {
        MDC.remove(MDC_CORRELATION_ID_KEY);
      }
    }
  }
}
