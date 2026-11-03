package com.ecommerce.shared.security;

import com.ecommerce.shared.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility methods for inspecting current authentication state and user authorities.
 */
public final class SecurityUtils {

  private SecurityUtils() {}

  public static boolean isAdmin() {
    return hasRole(UserRole.ADMIN);
  }

  public static boolean hasRole(UserRole role) {
    if (role == null) {
      return false;
    }
    return hasAuthority(role.getAuthority());
  }

  public static boolean hasAuthority(String authority) {
    if (authority == null) {
      return false;
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return false;
    }
    return auth.getAuthorities().stream()
        .anyMatch(a -> authority.equals(a.getAuthority()));
  }
}
