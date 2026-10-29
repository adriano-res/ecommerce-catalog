package com.ecommerce.support;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

  static final MongoDBContainer MONGO_CONTAINER;
  static final GenericContainer<?> REDIS_CONTAINER;

  static {
    MONGO_CONTAINER = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
        .withReuse(true);
    MONGO_CONTAINER.start();

    REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
        .withExposedPorts(6379)
        .withReuse(true);
    REDIS_CONTAINER.start();
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", () -> MONGO_CONTAINER.getReplicaSetUrl("catalog_test"));
    registry.add("spring.data.redis.url", () -> "redis://" + REDIS_CONTAINER.getHost() + ":" + REDIS_CONTAINER.getFirstMappedPort());
  }

  @Autowired
  protected MongoTemplate mongoTemplate;

  @Autowired
  protected RedisTemplate<String, Object> redisTemplate;

  @AfterEach
  void cleanUp() {
    if (mongoTemplate != null && mongoTemplate.getDb() != null) {
      mongoTemplate.getCollectionNames().forEach(name -> {
        if (!name.startsWith("system.")) {
          mongoTemplate.dropCollection(name);
        }
      });
    }
    if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
      redisTemplate.execute((RedisCallback<Void>) connection -> {
        connection.serverCommands().flushDb();
        return null;
      });
    }
  }
}
