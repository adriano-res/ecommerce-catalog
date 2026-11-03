package com.ecommerce.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.shared.enums.UserRole;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilsTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Should return false when security context has no authentication")
  void shouldReturnFalse_whenNoAuthenticationPresent() {
    SecurityContextHolder.clearContext();
    assertThat(SecurityUtils.isAdmin()).isFalse();
    assertThat(SecurityUtils.hasRole(UserRole.ADMIN)).isFalse();
    assertThat(SecurityUtils.hasAuthority("ROLE_ADMIN")).isFalse();
  }

  @Test
  @DisplayName("Should return true for isAdmin when authenticated user has ROLE_ADMIN")
  void shouldReturnTrue_whenUserHasAdminRole() {
    var auth =
        new UsernamePasswordAuthenticationToken(
            "admin-user", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertThat(SecurityUtils.isAdmin()).isTrue();
    assertThat(SecurityUtils.hasRole(UserRole.ADMIN)).isTrue();
    assertThat(SecurityUtils.hasRole(UserRole.USER)).isFalse();
  }

  @Test
  @DisplayName("Should return false for isAdmin when authenticated user has only ROLE_USER")
  void shouldReturnFalse_whenUserHasOnlyUserRole() {
    var auth =
        new UsernamePasswordAuthenticationToken(
            "regular-user", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertThat(SecurityUtils.isAdmin()).isFalse();
    assertThat(SecurityUtils.hasRole(UserRole.USER)).isTrue();
  }
}
