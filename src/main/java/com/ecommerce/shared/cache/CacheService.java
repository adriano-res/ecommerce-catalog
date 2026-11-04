package com.ecommerce.shared.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

  private static final Logger log = LoggerFactory.getLogger(CacheService.class);

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;

  public CacheService(
      StringRedisTemplate redisTemplate,
      ObjectMapper objectMapper,
      MeterRegistry meterRegistry) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    this.meterRegistry = meterRegistry;
  }

  public <T> Optional<T> get(String key, Class<T> targetClass) {
    try {
      String json = redisTemplate.opsForValue().get(key);
      if (json != null && !json.isBlank()) {
        recordHit(key);
        return Optional.ofNullable(objectMapper.readValue(json, targetClass));
      }
      recordMiss(key);
    } catch (Exception e) {
      recordError(key);
      log.warn("Failed to retrieve cache for key '{}': {}. Proceeding with fallback.", key, e.getMessage());
    }
    return Optional.empty();
  }

  public <T> Optional<T> get(String key, TypeReference<T> typeReference) {
    try {
      String json = redisTemplate.opsForValue().get(key);
      if (json != null && !json.isBlank()) {
        recordHit(key);
        return Optional.ofNullable(objectMapper.readValue(json, typeReference));
      }
      recordMiss(key);
    } catch (Exception e) {
      recordError(key);
      log.warn("Failed to retrieve cache for key '{}': {}. Proceeding with fallback.", key, e.getMessage());
    }
    return Optional.empty();
  }

  public void put(String key, Object value, Duration ttl) {
    try {
      String json = objectMapper.writeValueAsString(value);
      if (ttl != null) {
        redisTemplate.opsForValue().set(key, json, ttl);
      } else {
        redisTemplate.opsForValue().set(key, json);
      }
    } catch (Exception e) {
      recordError(key);
      log.warn("Failed to put cache for key '{}': {}. Proceeding without cache.", key, e.getMessage());
    }
  }

  public void evict(String key) {
    try {
      redisTemplate.delete(key);
      log.info("Evicted cache key: {}", key);
    } catch (Exception e) {
      recordError(key);
      log.warn("Failed to evict cache for key '{}': {}", key, e.getMessage());
    }
  }

  private void recordHit(String key) {
    meterRegistry.counter("catalog.cache.hits", "cache", resolveCacheName(key)).increment();
  }

  private void recordMiss(String key) {
    meterRegistry.counter("catalog.cache.misses", "cache", resolveCacheName(key)).increment();
  }

  private void recordError(String key) {
    meterRegistry.counter("catalog.cache.errors", "cache", resolveCacheName(key)).increment();
  }

  private String resolveCacheName(String key) {
    if (key == null) {
      return "default";
    }
    if (key.contains("product")) {
      return "product";
    }
    if (key.contains("category")) {
      return "category";
    }
    return "default";
  }
}
