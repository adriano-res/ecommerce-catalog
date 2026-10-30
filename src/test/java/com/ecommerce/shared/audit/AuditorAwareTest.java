package com.ecommerce.shared.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AuditorAwareTest {

  private AuditorAwareImpl auditorAware;

  @BeforeEach
  void setUp() {
    auditorAware = new AuditorAwareImpl();
    SecurityContextHolder.clearContext();
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    MDC.clear();
  }

  @Test
  @DisplayName("Should return authenticated username when SecurityContext has authentication")
  void shouldReturnAuthenticatedUsername_whenSecurityContextIsPopulated() {
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        "admin:carlos",
        null,
        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertThat(auditorAware.getCurrentAuditor()).contains("admin:carlos");
  }

  @Test
  @DisplayName("Should return actor from MDC when SecurityContext is empty")
  void shouldReturnActorFromMdc_whenSecurityContextIsEmpty() {
    MDC.put("actor", "mdc-user");

    assertThat(auditorAware.getCurrentAuditor()).contains("mdc-user");
  }

  @Test
  @DisplayName("Should return system fallback when neither SecurityContext nor MDC is set")
  void shouldReturnDefaultSystem_whenNoActorPresent() {
    assertThat(auditorAware.getCurrentAuditor()).contains("system");
  }
}
