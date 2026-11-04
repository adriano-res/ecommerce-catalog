package com.ecommerce.product;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.category.CategoryRepository;
import com.ecommerce.category.dto.AttributeDefinition;
import com.ecommerce.category.dto.AttributeDefinition.AttributeType;
import com.ecommerce.category.entity.CategoryDocument;
import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.shared.audit.AuditMetadata;
import com.ecommerce.shared.cache.CacheService;
import com.ecommerce.shared.enums.UserRole;
import com.ecommerce.shared.money.Money;
import com.ecommerce.shared.security.JwtService;
import com.ecommerce.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ProductCacheFallbackIT extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ProductRepository productRepository;

  @Autowired private CategoryRepository categoryRepository;

  @Autowired private JwtService jwtService;

  @Autowired private ObjectMapper objectMapper;

  @SpyBean private CacheService cacheService;

  private String adminToken;
  private CategoryDocument category;

  @BeforeEach
  void setUp() {
    productRepository.deleteAll();
    categoryRepository.deleteAll();

    adminToken = jwtService.generateToken("admin-tester", UserRole.ADMIN);

    AuditMetadata audit = AuditMetadata.create("admin-tester");
    CategoryDocument rootCat =
        new CategoryDocument(
            null,
            "smartphones",
            "Smartphones",
            null,
            List.of(),
            List.of(
                new AttributeDefinition(
                    "ram", "RAM", AttributeType.ENUM, "GB", true, true, List.of("8GB", "16GB")),
                new AttributeDefinition(
                    "storageGb", "Storage", AttributeType.NUMBER, "GB", true, true, List.of())),
            audit);
    category = categoryRepository.save(rootCat);
  }

  @Test
  @DisplayName("Should fallback to Mongo and return OK when Redis fails on get or put")
  void shouldFallbackToMongoAndReturnOk_whenRedisFailsOnGetOrPut() throws Exception {
    String productId = createAndPublishProduct("GALAXY-FALLBACK-1", "galaxy-fallback-1", "Galaxy Fallback One");

    doThrow(new RedisConnectionFailureException("Simulated Redis outage"))
        .when(cacheService)
        .get(anyString(), any(Class.class));

    doThrow(new RedisConnectionFailureException("Simulated Redis outage"))
        .when(cacheService)
        .put(anyString(), any(), any());

    mockMvc
        .perform(get("/api/v1/products/" + productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(productId)))
        .andExpect(jsonPath("$.name", is("Galaxy Fallback One")));

    mockMvc
        .perform(get("/api/v1/products/galaxy-fallback-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(productId)))
        .andExpect(jsonPath("$.slug", is("galaxy-fallback-1")));
  }

  @Test
  @DisplayName("Should return fresh product from Mongo without error when Redis is unavailable")
  void shouldReturnFreshProductFromMongoWithoutError_whenRedisIsUnavailable() throws Exception {
    String productId = createAndPublishProduct("GALAXY-OUTAGE-2", "galaxy-outage-2", "Galaxy Resilient");

    doThrow(new RuntimeException("Simulated unexpected Redis cluster failure"))
        .when(cacheService)
        .get(anyString(), any(Class.class));

    doThrow(new RuntimeException("Simulated unexpected Redis cluster failure"))
        .when(cacheService)
        .put(anyString(), any(), any());

    mockMvc
        .perform(get("/api/v1/products/" + productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(productId)))
        .andExpect(jsonPath("$.name", is("Galaxy Resilient")));
  }

  private String createAndPublishProduct(String sku, String slug, String name) throws Exception {
    CreateProductRequest createReq =
        new CreateProductRequest(
            sku,
            slug,
            name,
            "Brand",
            "Description",
            category.getId(),
            Money.brl("1999.00"),
            Map.of("ram", "8GB", "storageGb", 128),
            List.of());

    String createRes =
        mockMvc
            .perform(
                post("/api/v1/products")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String productId = objectMapper.readTree(createRes).get("id").asText();

    mockMvc
        .perform(
            post("/api/v1/products/" + productId + "/publish")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());

    return productId;
  }
}
