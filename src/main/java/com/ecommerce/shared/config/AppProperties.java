package com.ecommerce.shared.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    @Valid @NotNull DatasourceProperties datasource,
    @Valid @NotNull JwtProperties jwt,
    @Valid @NotNull CacheProperties cache) {

  public AppProperties {
    if (datasource == null) {
      datasource = new DatasourceProperties(
          "mongodb://localhost:27017/catalog?replicaSet=rs0",
          "redis://localhost:6379");
    }
    if (jwt == null) {
      jwt = new JwtProperties("dev-secret-key-must-be-at-least-256-bits-long-for-hmac-sha-256", 86400L);
    }
    if (cache == null) {
      cache = new CacheProperties(300L, 10L, 30L);
    }
  }

  public record DatasourceProperties(
      @NotBlank(message = "MongoDB URI must not be blank")
      String mongodbUri,
      @NotBlank(message = "Redis URL must not be blank")
      String redisUrl) {}

  public record JwtProperties(
      @NotBlank(message = "JWT secret must not be blank")
      String secret,
      @Positive(message = "JWT expiration must be positive")
      long expirationSeconds) {}

  public record CacheProperties(
      @Positive(message = "Default TTL must be positive")
      long defaultTtlSeconds,
      @Positive(message = "Product TTL must be positive")
      long productTtlMinutes,
      @Positive(message = "Category tree TTL must be positive")
      long categoryTreeTtlMinutes) {}
}
