package com.ecommerce.shared.security;

import com.ecommerce.shared.config.AppProperties;
import com.ecommerce.shared.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey signingKey;
  private final JwtParser jwtParser;
  private final long expirationSeconds;

  public JwtService(AppProperties appProperties) {
    String secret = appProperties.jwt().secret();
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.jwtParser = Jwts.parser().verifyWith(this.signingKey).build();
    this.expirationSeconds = appProperties.jwt().expirationSeconds();
  }

  public String generateToken(String username, UserRole role) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(username)
        .claim("role", role.getAuthority())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(expirationSeconds)))
        .signWith(signingKey)
        .compact();
  }

  public Claims extractClaims(String token) {
    return jwtParser.parseSignedClaims(token).getPayload();
  }

  public boolean validateToken(String token) {
    return parseValidClaims(token).isPresent();
  }

  public Optional<Claims> parseValidClaims(String token) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }
    try {
      Claims claims = extractClaims(token);
      Date expiration = claims.getExpiration();
      if (expiration != null && expiration.after(new Date())) {
        return Optional.of(claims);
      }
      return Optional.empty();
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  public Authentication getAuthentication(String token) {
    Claims claims = extractClaims(token);
    return buildAuthentication(claims);
  }

  public Authentication buildAuthentication(Claims claims) {
    String username = claims.getSubject();
    String role = claims.get("role", String.class);

    List<SimpleGrantedAuthority> authorities = (role != null && !role.isBlank())
        ? List.of(new SimpleGrantedAuthority(role))
        : Collections.emptyList();

    return new UsernamePasswordAuthenticationToken(username, null, authorities);
  }

  public long getExpirationSeconds() {
    return expirationSeconds;
  }
}
