package com.ecommerce.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.category.dto.CategoryTreeResponse;
import com.ecommerce.category.dto.CreateCategoryRequest;
import com.ecommerce.shared.cache.CacheService;
import com.ecommerce.shared.enums.UserRole;
import com.ecommerce.shared.security.JwtService;
import com.ecommerce.support.AbstractIntegrationTest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CategoryCacheIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private CacheService cacheService;

  @Autowired
  private JwtService jwtService;

  @Autowired
  private ObjectMapper objectMapper;

  private String adminToken;

  @BeforeEach
  void setUp() {
    categoryRepository.deleteAll();
    cacheService.evict(CategoryService.CACHE_KEY_TREE);
    adminToken = jwtService.generateToken("admin-tester", UserRole.ADMIN);
  }

  @Test
  @DisplayName("Should populate Redis cache on tree query and invalidate cache when new category is created")
  void shouldPopulateCacheOnTreeQuery_andEvictOnCategoryCreation() throws Exception {
    CreateCategoryRequest rootReq = new CreateCategoryRequest("Eletrônicos", "eletronicos", null, List.of());
    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(rootReq)))
        .andExpect(status().isCreated());

    assertThat(cacheService.get(CategoryService.CACHE_KEY_TREE, new TypeReference<List<CategoryTreeResponse>>() {})).isEmpty();

    mockMvc.perform(get("/api/v1/categories/tree"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].slug", is("eletronicos")));

    var cached = cacheService.get(CategoryService.CACHE_KEY_TREE, new TypeReference<List<CategoryTreeResponse>>() {});
    assertThat(cached).isPresent();
    assertThat(cached.get()).hasSize(1);
    assertThat(cached.get().get(0).slug()).isEqualTo("eletronicos");

    CreateCategoryRequest subReq = new CreateCategoryRequest("Smartphones", "smartphones", null, List.of());
    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(subReq)))
        .andExpect(status().isCreated());

    assertThat(cacheService.get(CategoryService.CACHE_KEY_TREE, new TypeReference<List<CategoryTreeResponse>>() {})).isEmpty();

    mockMvc.perform(get("/api/v1/categories/tree"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));

    assertThat(cacheService.get(CategoryService.CACHE_KEY_TREE, new TypeReference<List<CategoryTreeResponse>>() {})).isPresent();
  }

  @Test
  @DisplayName("Should evict Redis cache when category is deleted")
  void shouldEvictCacheOnCategoryDeletion() throws Exception {
    CreateCategoryRequest req = new CreateCategoryRequest("Casa", "casa", null, List.of());
    String resJson = mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String id = objectMapper.readTree(resJson).get("id").asText();

    mockMvc.perform(get("/api/v1/categories/tree"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));

    assertThat(cacheService.get(CategoryService.CACHE_KEY_TREE, new TypeReference<List<CategoryTreeResponse>>() {})).isPresent();

    mockMvc.perform(delete("/api/v1/categories/" + id)
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    assertThat(cacheService.get(CategoryService.CACHE_KEY_TREE, new TypeReference<List<CategoryTreeResponse>>() {})).isEmpty();
  }
}
