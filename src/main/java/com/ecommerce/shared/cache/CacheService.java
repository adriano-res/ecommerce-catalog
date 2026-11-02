package com.ecommerce.shared.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  public CacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  public <T> Optional<T> get(String key, Class<T> targetClass) {
    try {
      String json = redisTemplate.opsForValue().get(key);
      if (json != null && !json.isBlank()) {
        return Optional.ofNullable(objectMapper.readValue(json, targetClass));
      }
    } catch (Exception e) {
      log.warn("Failed to retrieve cache for key '{}': {}. Proceeding with fallback.", key, e.getMessage());
    }
    return Optional.empty();
  }

  public <T> Optional<T> get(String key, TypeReference<T> typeReference) {
    try {
      String json = redisTemplate.opsForValue().get(key);
      if (json != null && !json.isBlank()) {
        return Optional.ofNullable(objectMapper.readValue(json, typeReference));
      }
    } catch (Exception e) {
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
      log.warn("Failed to put cache for key '{}': {}. Proceeding without cache.", key, e.getMessage());
    }
  }

  public void evict(String key) {
    try {
      redisTemplate.delete(key);
      log.info("Evicted cache key: {}", key);
    } catch (Exception e) {
      log.warn("Failed to evict cache for key '{}': {}", key, e.getMessage());
    }
  }
}
