package com.ecommerce.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.category.dto.AttributeDefinition;
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
class CategoryHierarchyIT extends AbstractIntegrationTest {

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
  @DisplayName("Should create root and subcategory, persist ancestors and inherit attributes")
  void shouldCreateHierarchyAndInheritAttributes_whenValidRequests() throws Exception {
    AttributeDefinition voltage = new AttributeDefinition("voltage", "Voltage", AttributeDefinition.AttributeType.STRING, "V", false, true, List.of());
    CreateCategoryRequest rootReq = new CreateCategoryRequest("Eletrônicos", "eletronicos", null, List.of(voltage));

    String rootResJson = mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(rootReq)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.slug", is("eletronicos")))
        .andExpect(jsonPath("$.ancestors", hasSize(0)))
        .andExpect(jsonPath("$.effectiveAttributeDefinitions", hasSize(1)))
        .andReturn().getResponse().getContentAsString();

    String rootId = objectMapper.readTree(rootResJson).get("id").asText();

    AttributeDefinition ram = new AttributeDefinition("ram", "RAM", AttributeDefinition.AttributeType.NUMBER, "GB", false, true, List.of());
    CreateCategoryRequest subReq = new CreateCategoryRequest("Smartphones", "smartphones", rootId, List.of(ram));

    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(subReq)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.slug", is("smartphones")))
        .andExpect(jsonPath("$.ancestors", hasSize(1)))
        .andExpect(jsonPath("$.ancestors[0]", is(rootId)))
        .andExpect(jsonPath("$.effectiveAttributeDefinitions", hasSize(2)))
        .andExpect(jsonPath("$.effectiveAttributeDefinitions[0].key", is("voltage")))
        .andExpect(jsonPath("$.effectiveAttributeDefinitions[1].key", is("ram")));

    mockMvc.perform(get("/api/v1/categories/tree"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].slug", is("eletronicos")))
        .andExpect(jsonPath("$[0].children", hasSize(1)))
        .andExpect(jsonPath("$[0].children[0].slug", is("smartphones")));

    assertThat(cacheService.get(CategoryService.CACHE_KEY_TREE, new TypeReference<List<CategoryTreeResponse>>() {})).isPresent();
  }

  @Test
  @DisplayName("Should return category by ID when category exists")
  void shouldReturnCategoryById_whenCategoryExists() throws Exception {
    AttributeDefinition color = new AttributeDefinition("color", "Color", AttributeDefinition.AttributeType.STRING, null, false, true, List.of());
    CreateCategoryRequest req = new CreateCategoryRequest("Moda", "moda", null, List.of(color));

    String resJson = mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String id = objectMapper.readTree(resJson).get("id").asText();

    mockMvc.perform(get("/api/v1/categories/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(id)))
        .andExpect(jsonPath("$.slug", is("moda")))
        .andExpect(jsonPath("$.name", is("Moda")))
        .andExpect(jsonPath("$.effectiveAttributeDefinitions", hasSize(1)))
        .andExpect(jsonPath("$.effectiveAttributeDefinitions[0].key", is("color")));
  }

  @Test
  @DisplayName("Should return category by slug when category exists")
  void shouldReturnCategoryBySlug_whenCategoryExists() throws Exception {
    CreateCategoryRequest req = new CreateCategoryRequest("Livros", "livros", null, List.of());

    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/v1/categories/livros"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slug", is("livros")))
        .andExpect(jsonPath("$.name", is("Livros")));
  }

  @Test
  @DisplayName("Should return 404 when category does not exist by id or slug")
  void shouldReturn404_whenCategoryDoesNotExist() throws Exception {
    mockMvc.perform(get("/api/v1/categories/inexistente"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.failureCode", is("CATEGORY_NOT_FOUND")));
  }

  @Test
  @DisplayName("Should delete leaf category and return 204 when category has no subcategories")
  void shouldDeleteLeafCategory_whenNoChildren() throws Exception {
    CreateCategoryRequest req = new CreateCategoryRequest("Games", "games", null, List.of());

    String resJson = mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String id = objectMapper.readTree(resJson).get("id").asText();

    mockMvc.perform(delete("/api/v1/categories/" + id)
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/v1/categories/" + id))
        .andExpect(status().isNotFound());
  }
}
