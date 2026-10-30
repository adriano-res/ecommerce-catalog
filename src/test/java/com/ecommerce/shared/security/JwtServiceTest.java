package com.ecommerce.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.shared.config.AppProperties;
import com.ecommerce.shared.enums.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class JwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    AppProperties.DatasourceProperties datasource = new AppProperties.DatasourceProperties(
        "mongodb://localhost:27017/catalog",
        "redis://localhost:6379");
    AppProperties.JwtProperties jwt = new AppProperties.JwtProperties(
        "a-very-long-and-secure-test-secret-key-at-least-256-bits-long",
        3600L);
    AppProperties.CacheProperties cache = new AppProperties.CacheProperties(300L, 10L, 30L);
    AppProperties appProperties = new AppProperties(datasource, jwt, cache);

    jwtService = new JwtService(appProperties);
  }

  @Test
  @DisplayName("Should generate valid token with ROLE_ADMIN when ADMIN role requested")
  void shouldGenerateValidToken_whenAdminRoleRequested() {
    String token = jwtService.generateToken("admin-user", UserRole.ADMIN);

    assertThat(token).isNotEmpty();
    assertThat(jwtService.validateToken(token)).isTrue();

    Claims claims = jwtService.extractClaims(token);
    assertThat(claims.getSubject()).isEqualTo("admin-user");
    assertThat(claims.get("role", String.class)).isEqualTo("ROLE_ADMIN");

    Authentication auth = jwtService.getAuthentication(token);
    assertThat(auth.getName()).isEqualTo("admin-user");
    assertThat(auth.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_ADMIN");
  }

  @Test
  @DisplayName("Should generate valid token with ROLE_USER when USER role requested")
  void shouldGenerateValidToken_whenUserRoleRequested() {
    String token = jwtService.generateToken("normal-user", UserRole.USER);

    assertThat(token).isNotEmpty();
    assertThat(jwtService.validateToken(token)).isTrue();

    Claims claims = jwtService.extractClaims(token);
    assertThat(claims.getSubject()).isEqualTo("normal-user");
    assertThat(claims.get("role", String.class)).isEqualTo("ROLE_USER");

    Authentication auth = jwtService.getAuthentication(token);
    assertThat(auth.getName()).isEqualTo("normal-user");
    assertThat(auth.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_USER");
  }

  @Test
  @DisplayName("Should return false when validating invalid token")
  void shouldReturnFalse_whenTokenIsInvalid() {
    assertThat(jwtService.validateToken("not.a.valid.jwt.token")).isFalse();
    assertThat(jwtService.validateToken("")).isFalse();
    assertThat(jwtService.validateToken(null)).isFalse();
  }

  @Test
  @DisplayName("Should return false when validating expired token")
  void shouldReturnFalse_whenTokenIsExpired() {
    AppProperties.DatasourceProperties datasource = new AppProperties.DatasourceProperties(
        "mongodb://localhost:27017/catalog",
        "redis://localhost:6379");
    AppProperties.JwtProperties expiredJwt = new AppProperties.JwtProperties(
        "a-very-long-and-secure-test-secret-key-at-least-256-bits-long",
        -10L);
    AppProperties.CacheProperties cache = new AppProperties.CacheProperties(300L, 10L, 30L);
    AppProperties expiredProperties = new AppProperties(datasource, expiredJwt, cache);
    JwtService expiredJwtService = new JwtService(expiredProperties);

    String expiredToken = expiredJwtService.generateToken("admin-user", UserRole.ADMIN);

    assertThat(jwtService.validateToken(expiredToken)).isFalse();
  }

  @Test
  @DisplayName("Should return false when validating token signed with different key")
  void shouldReturnFalse_whenTokenSignedWithDifferentKey() {
    AppProperties.DatasourceProperties datasource = new AppProperties.DatasourceProperties(
        "mongodb://localhost:27017/catalog",
        "redis://localhost:6379");
    AppProperties.JwtProperties differentJwt = new AppProperties.JwtProperties(
        "a-completely-different-secret-key-at-least-256-bits-long-xyz",
        3600L);
    AppProperties.CacheProperties cache = new AppProperties.CacheProperties(300L, 10L, 30L);
    AppProperties differentProperties = new AppProperties(datasource, differentJwt, cache);
    JwtService differentJwtService = new JwtService(differentProperties);

    String tokenFromOtherKey = differentJwtService.generateToken("admin-user", UserRole.ADMIN);

    assertThat(jwtService.validateToken(tokenFromOtherKey)).isFalse();
  }
}
