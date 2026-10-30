package com.ecommerce.auth.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public record DevTokenRequest(
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    String role,
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    String username
) {
  public String resolvedRole() {
    return (role != null && !role.isBlank()) ? role : "ADMIN";
  }

  public String resolvedUsername() {
    return (username != null && !username.isBlank()) ? username : "admin";
  }
}
