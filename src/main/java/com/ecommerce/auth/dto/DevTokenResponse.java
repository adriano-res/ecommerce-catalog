package com.ecommerce.auth.dto;

public record DevTokenResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    String role,
    String username
) {}
