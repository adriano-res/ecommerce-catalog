package com.ecommerce.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload containing the generated JWT bearer token")
public record DevTokenResponse(
    @Schema(
        description = "Signed JWT bearer token",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String accessToken,
    @Schema(description = "Authorization scheme type", example = "Bearer")
    String tokenType,
    @Schema(description = "Token validity lifetime in seconds", example = "86400")
    long expiresIn,
    @Schema(description = "Granted authority name", example = "ROLE_ADMIN")
    String role,
    @Schema(description = "Authenticated principal username", example = "admin")
    String username) {}
