package com.ecommerce.shared.enums;

import java.util.Locale;

public enum UserRole {
  ADMIN("ROLE_ADMIN"),
  USER("ROLE_USER");

  private final String authority;

  UserRole(String authority) {
    this.authority = authority;
  }

  public String getAuthority() {
    return authority;
  }

  public static UserRole fromString(String role) {
    if (role == null || role.isBlank()) {
      throw new IllegalArgumentException("Role must not be blank. Allowed values: ADMIN, USER");
    }
    String normalized = role.trim().toUpperCase(Locale.ROOT);
    if (normalized.startsWith("ROLE_")) {
      normalized = normalized.substring(5);
    }
    try {
      return UserRole.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown role: " + role + ". Allowed values: ADMIN, USER");
    }
  }
}
