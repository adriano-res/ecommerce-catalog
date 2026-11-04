package com.ecommerce.shared.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class CacheServiceTest {

  private StringRedisTemplate redisTemplate;
  private ValueOperations<String, String> valueOperations;
  private ObjectMapper objectMapper;
  private MeterRegistry meterRegistry;
  private CacheService cacheService;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    redisTemplate = mock(StringRedisTemplate.class);
    valueOperations = mock(ValueOperations.class);
    objectMapper = mock(ObjectMapper.class);
    meterRegistry = new SimpleMeterRegistry();

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    cacheService = new CacheService(redisTemplate, objectMapper, meterRegistry);
  }

  @Test
  @DisplayName("Should return cached value when cache key exists")
  void shouldReturnCachedValue_whenCacheKeyExists() throws Exception {
    String json = "{\"id\":\"123\",\"name\":\"Electronics\"}";
    TestDto expected = new TestDto("123", "Electronics");

    when(valueOperations.get("test:key")).thenReturn(json);
    when(objectMapper.readValue(json, TestDto.class)).thenReturn(expected);

    Optional<TestDto> result = cacheService.get("test:key", TestDto.class);

    assertThat(result).contains(expected);
    assertThat(meterRegistry.get("catalog.cache.hits").tag("cache", "default").counter().count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Should return empty optional when cache key does not exist")
  void shouldReturnEmptyOptional_whenCacheKeyDoesNotExist() {
    when(valueOperations.get("cache:product:miss")).thenReturn(null);

    Optional<TestDto> result = cacheService.get("cache:product:miss", TestDto.class);

    assertThat(result).isEmpty();
    assertThat(meterRegistry.get("catalog.cache.misses").tag("cache", "product").counter().count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Should return empty optional and record error when redis connection fails on get")
  void shouldReturnEmptyOptional_whenRedisFailsOnGet() {
    when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("Redis down"));

    Optional<TestDto> result = cacheService.get("cache:category:fail", TestDto.class);

    assertThat(result).isEmpty();
    assertThat(meterRegistry.get("catalog.cache.errors").tag("cache", "category").counter().count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Should return cached list when type reference matches")
  void shouldReturnCachedList_whenTypeReferenceMatches() throws Exception {
    String json = "[{\"id\":\"1\",\"name\":\"A\"}]";
    List<TestDto> expectedList = List.of(new TestDto("1", "A"));

    when(valueOperations.get("cache:product:list")).thenReturn(json);
    when(objectMapper.readValue(eq(json), any(TypeReference.class))).thenReturn(expectedList);

    Optional<List<TestDto>> result = cacheService.get("cache:product:list", new TypeReference<List<TestDto>>() {});

    assertThat(result).isPresent();
    assertThat(result.get()).hasSize(1);
    assertThat(result.get().get(0).name()).isEqualTo("A");
    assertThat(meterRegistry.get("catalog.cache.hits").tag("cache", "product").counter().count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Should serialize and store in redis when put is called with ttl")
  void shouldStoreInRedis_whenPutIsCalledWithTtl() throws Exception {
    TestDto dto = new TestDto("1", "Item");
    when(objectMapper.writeValueAsString(dto)).thenReturn("{\"id\":\"1\",\"name\":\"Item\"}");

    cacheService.put("test:key", dto, Duration.ofMinutes(10));

    verify(valueOperations).set("test:key", "{\"id\":\"1\",\"name\":\"Item\"}", Duration.ofMinutes(10));
  }

  @Test
  @DisplayName("Should handle serialization or redis error gracefully when put fails")
  void shouldHandleGracefully_whenPutFails() throws Exception {
    TestDto dto = new TestDto("1", "Item");
    when(objectMapper.writeValueAsString(dto)).thenReturn("{\"id\":\"1\",\"name\":\"Item\"}");
    doThrow(new RedisConnectionFailureException("Redis down"))
        .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

    cacheService.put("cache:product:key", dto, Duration.ofMinutes(10));

    assertThat(meterRegistry.get("catalog.cache.errors").tag("cache", "product").counter().count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Should delete key in redis when evict is called")
  void shouldDeleteKeyInRedis_whenEvictIsCalled() {
    cacheService.evict("test:key");

    verify(redisTemplate).delete("test:key");
  }

  @Test
  @DisplayName("Should handle redis failure gracefully when evict fails")
  void shouldHandleGracefully_whenEvictFails() {
    doThrow(new RedisConnectionFailureException("Redis down"))
        .when(redisTemplate).delete(anyString());

    cacheService.evict("cache:category:key");

    assertThat(meterRegistry.get("catalog.cache.errors").tag("cache", "category").counter().count()).isEqualTo(1.0);
  }

  private record TestDto(String id, String name) {}
}
