package com.ecommerce.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.category.CategoryRepository;
import com.ecommerce.category.dto.AttributeDefinition;
import com.ecommerce.category.dto.AttributeDefinition.AttributeType;
import com.ecommerce.category.entity.CategoryDocument;
import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.UpdateProductRequest;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.shared.audit.AuditMetadata;
import com.ecommerce.shared.cache.CacheService;
import com.ecommerce.shared.enums.UserRole;
import com.ecommerce.shared.money.Money;
import com.ecommerce.shared.security.JwtService;
import com.ecommerce.shared.storage.StorageService;
import com.ecommerce.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ProductCacheIT extends AbstractIntegrationTest {

  private static final String CACHE_PREFIX = "cache:product:";

  @Autowired private MockMvc mockMvc;

  @Autowired private ProductRepository productRepository;

  @Autowired private CategoryRepository categoryRepository;

  @Autowired private CacheService cacheService;

  @Autowired private StringRedisTemplate stringRedisTemplate;

  @Autowired private JwtService jwtService;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private StorageService storageService;

  private String adminToken;
  private CategoryDocument category;

  @BeforeEach
  void setUp() {
    productRepository.deleteAll();
    categoryRepository.deleteAll();

    when(storageService.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
        .thenAnswer(inv -> "http://localhost:8333/ecommerce-catalog/" + inv.getArgument(0));

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
  @DisplayName("Should return cached product and avoid database hit when subsequent request executed")
  void shouldReturnCachedProductAndAvoidDatabaseHit_whenSubsequentRequestExecuted() throws Exception {
    String productId = createAndPublishProduct("GALAXY-CACHE-1", "galaxy-cache-1", "Galaxy Cache One");

    String idKey = CACHE_PREFIX + productId;
    String slugKey = CACHE_PREFIX + "galaxy-cache-1";

    assertThat(stringRedisTemplate.hasKey(idKey)).isFalse();
    assertThat(stringRedisTemplate.hasKey(slugKey)).isFalse();

    mockMvc
        .perform(get("/api/v1/products/" + productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(productId)))
        .andExpect(jsonPath("$.name", is("Galaxy Cache One")));

    assertThat(stringRedisTemplate.hasKey(idKey)).isTrue();
    assertThat(stringRedisTemplate.hasKey(slugKey)).isTrue();

    Long ttlSeconds = stringRedisTemplate.getExpire(idKey, TimeUnit.SECONDS);
    assertThat(ttlSeconds).isNotNull().isGreaterThan(0).isLessThanOrEqualTo(600);

    Optional<ProductResponse> cached = cacheService.get(idKey, ProductResponse.class);
    assertThat(cached).isPresent();
    assertThat(cached.get().name()).isEqualTo("Galaxy Cache One");

    productRepository.deleteById(productId);
    assertThat(productRepository.existsById(productId)).isFalse();

    mockMvc
        .perform(get("/api/v1/products/" + productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(productId)))
        .andExpect(jsonPath("$.name", is("Galaxy Cache One")));

    mockMvc
        .perform(get("/api/v1/products/galaxy-cache-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(productId)))
        .andExpect(jsonPath("$.name", is("Galaxy Cache One")));
  }

  @Test
  @DisplayName("Should evict cache and return fresh product when product is updated")
  void shouldEvictCacheAndReturnFreshProduct_whenProductIsUpdated() throws Exception {
    String productId = createAndPublishProduct("GALAXY-EVICT-1", "galaxy-evict-1", "Initial Name");

    org.springframework.test.web.servlet.MvcResult initialGet =
        mockMvc
            .perform(get("/api/v1/products/" + productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", is("Initial Name")))
            .andReturn();

    String etag = initialGet.getResponse().getHeader("ETag");
    assertThat(etag).isNotNull();

    String idKey = CACHE_PREFIX + productId;
    String slugKey = CACHE_PREFIX + "galaxy-evict-1";
    assertThat(cacheService.get(idKey, ProductResponse.class)).isPresent();
    assertThat(cacheService.get(slugKey, ProductResponse.class)).isPresent();

    UpdateProductRequest updateReq =
        new UpdateProductRequest(
            "Updated Fresh Name",
            "Samsung",
            "Updated description",
            null,
            Money.brl("3299.00"),
            Map.of("ram", "8GB", "storageGb", 128),
            null);

    mockMvc
        .perform(
            patch("/api/v1/products/" + productId)
                .header("Authorization", "Bearer " + adminToken)
                .header("If-Match", etag)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Updated Fresh Name")));

    assertThat(cacheService.get(idKey, ProductResponse.class)).isEmpty();
    assertThat(cacheService.get(slugKey, ProductResponse.class)).isEmpty();

    mockMvc
        .perform(get("/api/v1/products/" + productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Updated Fresh Name")));

    assertThat(cacheService.get(idKey, ProductResponse.class)).isPresent();
    assertThat(cacheService.get(idKey, ProductResponse.class).get().name())
        .isEqualTo("Updated Fresh Name");
  }

  @Test
  @DisplayName("Should never cache draft or archived products when queried")
  void shouldNeverCacheDraftOrArchivedProducts_whenQueried() throws Exception {
    CreateProductRequest createReq =
        new CreateProductRequest(
            "SMART-DRAFT-NOCACHE",
            "draft-no-cache",
            "Draft No Cache Phone",
            "Brand",
            "Description",
            category.getId(),
            Money.brl("1500.00"),
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
    String idKey = CACHE_PREFIX + productId;
    String slugKey = CACHE_PREFIX + "draft-no-cache";

    mockMvc
        .perform(get("/api/v1/products/" + productId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.failureCode", is("PRODUCT_NOT_FOUND")));

    assertThat(cacheService.get(idKey, ProductResponse.class)).isEmpty();
    assertThat(cacheService.get(slugKey, ProductResponse.class)).isEmpty();

    mockMvc
        .perform(
            get("/api/v1/products/" + productId).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("DRAFT")));

    assertThat(cacheService.get(idKey, ProductResponse.class)).isEmpty();
    assertThat(cacheService.get(slugKey, ProductResponse.class)).isEmpty();

    mockMvc
        .perform(
            post("/api/v1/products/" + productId + "/publish")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/products/" + productId + "/archive")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("ARCHIVED")));

    mockMvc
        .perform(get("/api/v1/products/" + productId))
        .andExpect(status().isNotFound());

    assertThat(cacheService.get(idKey, ProductResponse.class)).isEmpty();
    assertThat(cacheService.get(slugKey, ProductResponse.class)).isEmpty();

    mockMvc
        .perform(
            get("/api/v1/products/" + productId).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("ARCHIVED")));

    assertThat(cacheService.get(idKey, ProductResponse.class)).isEmpty();
    assertThat(cacheService.get(slugKey, ProductResponse.class)).isEmpty();
  }

  @Test
  @DisplayName("Should evict cache and return updated product when image is added")
  void shouldEvictCacheAndReturnUpdatedProduct_whenImageIsAdded() throws Exception {
    String productId = createAndPublishProduct("GALAXY-IMG-ADD", "galaxy-img-add", "Galaxy Image Add");
    String idKey = CACHE_PREFIX + productId;
    String slugKey = CACHE_PREFIX + "galaxy-img-add";

    mockMvc
        .perform(get("/api/v1/products/" + productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.images").isEmpty());

    assertThat(cacheService.get(idKey, ProductResponse.class)).isPresent();
    assertThat(cacheService.get(slugKey, ProductResponse.class)).isPresent();

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "cover.png", "image/png", "sample-image-bytes".getBytes());

    mockMvc
        .perform(
            multipart("/api/v1/products/" + productId + "/images")
                .file(file)
                .param("alt", "Cover photo")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.images.length()", is(1)));

    assertThat(cacheService.get(idKey, ProductResponse.class)).isEmpty();
    assertThat(cacheService.get(slugKey, ProductResponse.class)).isEmpty();

    mockMvc
        .perform(get("/api/v1/products/" + productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.images.length()", is(1)))
        .andExpect(jsonPath("$.images[0].alt", is("Cover photo")));

    assertThat(cacheService.get(idKey, ProductResponse.class)).isPresent();
    assertThat(cacheService.get(idKey, ProductResponse.class).get().images()).hasSize(1);
  }

  @Test
  @DisplayName("Should evict cache and return updated product when image is removed")
  void shouldEvictCacheAndReturnUpdatedProduct_whenImageIsRemoved() throws Exception {
    String productId = createAndPublishProduct("GALAXY-IMG-REM", "galaxy-img-rem", "Galaxy Image Remove");
    String idKey = CACHE_PREFIX + productId;
    String slugKey = CACHE_PREFIX + "galaxy-img-rem";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "photo.png", "image/png", "sample-image-bytes".getBytes());

    String uploadResponse =
        mockMvc
            .perform(
                multipart("/api/v1/products/" + productId + "/images")
                    .file(file)
                    .param("alt", "To be removed")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String imageId = objectMapper.readTree(uploadResponse).get("images").get(0).get("id").asText();

    mockMvc
        .perform(get("/api/v1/products/" + productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.images.length()", is(1)));

    assertThat(cacheService.get(idKey, ProductResponse.class)).isPresent();
    assertThat(cacheService.get(idKey, ProductResponse.class).get().images()).hasSize(1);

    mockMvc
        .perform(
            delete("/api/v1/products/" + productId + "/images/" + imageId)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.images").isEmpty());

    assertThat(cacheService.get(idKey, ProductResponse.class)).isEmpty();
    assertThat(cacheService.get(slugKey, ProductResponse.class)).isEmpty();

    mockMvc
        .perform(get("/api/v1/products/" + productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.images").isEmpty());

    assertThat(cacheService.get(idKey, ProductResponse.class)).isPresent();
    assertThat(cacheService.get(idKey, ProductResponse.class).get().images()).isEmpty();
  }

  @Test
  @DisplayName("Should evict cache and return 404 when active product is archived")
  void shouldEvictCacheAndReturnNotFound_whenActiveProductIsArchived() throws Exception {
    String productId = createAndPublishProduct("GALAXY-ARCH-EVICT", "galaxy-arch-evict", "Galaxy To Archive");
    String idKey = CACHE_PREFIX + productId;
    String slugKey = CACHE_PREFIX + "galaxy-arch-evict";

    mockMvc
        .perform(get("/api/v1/products/" + productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(productId)));

    assertThat(cacheService.get(idKey, ProductResponse.class)).isPresent();

    mockMvc
        .perform(
            post("/api/v1/products/" + productId + "/archive")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("ARCHIVED")));

    assertThat(cacheService.get(idKey, ProductResponse.class)).isEmpty();
    assertThat(cacheService.get(slugKey, ProductResponse.class)).isEmpty();

    mockMvc
        .perform(get("/api/v1/products/" + productId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.failureCode", is("PRODUCT_NOT_FOUND")));
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
            Money.brl("2999.90"),
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
