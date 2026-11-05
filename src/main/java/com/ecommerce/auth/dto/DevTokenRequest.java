package com.ecommerce.auth.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for development JWT token generation")
public record DevTokenRequest(
    @Schema(
        description = "Target role authority (ADMIN or USER)",
        example = "ADMIN",
        defaultValue = "ADMIN")
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    String role,
    @Schema(
        description = "Target subject username",
        example = "admin",
        defaultValue = "admin")
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    String username) {

  public String resolvedRole() {
    return (role != null && !role.isBlank()) ? role : "ADMIN";
  }

  public String resolvedUsername() {
    return (username != null && !username.isBlank()) ? username : "admin";
  }
}
