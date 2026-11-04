package com.ecommerce.actuator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.ecommerce.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ActuatorHealthIT extends AbstractIntegrationTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @SpyBean
  private RedisConnectionFactory redisConnectionFactory;

  @AfterEach
  void resetMocks() {
    reset(redisConnectionFactory);
  }

  @Test
  @DisplayName("Should return 200 UP when calling liveness probe")
  void shouldReturn200Up_whenCallingLivenessProbe() {
    ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/liveness", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
  }

  @Test
  @DisplayName("Should return 200 UP when calling readiness probe with healthy dependencies")
  void shouldReturn200Up_whenCallingReadinessProbeWithHealthyDependencies() {
    ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/readiness", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
    assertThat(response.getBody()).contains("\"redisReadiness\"");
    assertThat(response.getBody()).contains("\"active-cache\"");
  }

  @Test
  @DisplayName("Should return 200 DEGRADED when redis connection fails during readiness probe")
  void shouldReturn200Degraded_whenRedisConnectionFailsDuringReadinessProbe() {
    doThrow(new RedisConnectionFailureException("Simulated Redis failure"))
        .when(redisConnectionFactory).getConnection();

    ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/readiness", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"DEGRADED\"");
    assertThat(response.getBody()).contains("\"cache-fallback\"");
    assertThat(response.getBody()).contains("\"status\":\"UNAVAILABLE\"");
  }
}
