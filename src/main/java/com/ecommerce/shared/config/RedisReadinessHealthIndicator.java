package com.ecommerce.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component("redisReadiness")
public class RedisReadinessHealthIndicator implements HealthIndicator {

  private static final Logger log = LoggerFactory.getLogger(RedisReadinessHealthIndicator.class);

  private final RedisConnectionFactory connectionFactory;

  public RedisReadinessHealthIndicator(RedisConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  @Override
  public Health health() {
    try (RedisConnection conn = connectionFactory.getConnection()) {
      conn.ping();
      return Health.up()
          .withDetail("status", "AVAILABLE")
          .withDetail("mode", "active-cache")
          .build();
    } catch (Exception e) {
      log.warn("Redis readiness health check failed: {}. Degraded mode active.", e.getMessage());
      return Health.status("DEGRADED")
          .withDetail("status", "UNAVAILABLE")
          .withDetail("mode", "cache-fallback")
          .withDetail("error", e.getMessage())
          .build();
    }
  }
}
