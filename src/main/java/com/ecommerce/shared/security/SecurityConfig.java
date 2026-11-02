package com.ecommerce.shared.security;

import com.ecommerce.shared.error.FailureCode;
import com.ecommerce.shared.error.ProblemDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtService jwtService;
  private final ObjectMapper objectMapper;

  public SecurityConfig(JwtService jwtService, ObjectMapper objectMapper) {
    this.jwtService = jwtService;
    this.objectMapper = objectMapper;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);

    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/dev-token").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**").permitAll()
            .requestMatchers("/actuator/**").permitAll()

            .requestMatchers(HttpMethod.POST, "/api/v1/categories", "/api/v1/categories/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/v1/categories", "/api/v1/categories/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/api/v1/categories", "/api/v1/categories/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/v1/categories", "/api/v1/categories/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/v1/products", "/api/v1/products/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/v1/products", "/api/v1/products/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/api/v1/products", "/api/v1/products/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/v1/products", "/api/v1/products/**").hasRole("ADMIN")

            .anyRequest().authenticated())
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(authenticationEntryPoint())
            .accessDeniedHandler(accessDeniedHandler()))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public AuthenticationEntryPoint authenticationEntryPoint() {
    return (request, response, authException) -> {
      ProblemDetail problem = ProblemDetails.build(
          HttpStatus.UNAUTHORIZED,
          FailureCode.UNAUTHORIZED,
          "Full authentication is required to access this resource",
          request);
      writeProblemDetail(response, problem, HttpStatus.UNAUTHORIZED);
    };
  }

  @Bean
  public AccessDeniedHandler accessDeniedHandler() {
    return (request, response, accessDeniedException) -> {
      ProblemDetail problem = ProblemDetails.build(
          HttpStatus.FORBIDDEN,
          FailureCode.FORBIDDEN,
          "Access denied: insufficient permissions to access this resource",
          request);
      writeProblemDetail(response, problem, HttpStatus.FORBIDDEN);
    };
  }

  private void writeProblemDetail(
      HttpServletResponse response,
      ProblemDetail problem,
      HttpStatus status) throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), problem);
  }
}
