package com.ecommerce.category;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.category.dto.AttributeDefinition;
import com.ecommerce.category.dto.CreateCategoryRequest;
import com.ecommerce.shared.cache.CacheService;
import com.ecommerce.shared.enums.UserRole;
import com.ecommerce.shared.security.JwtService;
import com.ecommerce.support.AbstractIntegrationTest;
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
class CategoryValidationIT extends AbstractIntegrationTest {

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
  private String userToken;

  @BeforeEach
  void setUp() {
    categoryRepository.deleteAll();
    cacheService.evict(CategoryService.CACHE_KEY_TREE);
    adminToken = jwtService.generateToken("admin-tester", UserRole.ADMIN);
    userToken = jwtService.generateToken("user-tester", UserRole.USER);
  }

  @Test
  @DisplayName("Should return 422 ProblemDetail when subcategory overrides inherited attribute key")
  void shouldReturn422_whenSubcategoryOverridesInheritedAttributeKey() throws Exception {
    AttributeDefinition brandRoot = new AttributeDefinition("brand", "Brand", AttributeDefinition.AttributeType.STRING, null, true, true, List.of());
    CreateCategoryRequest rootReq = new CreateCategoryRequest("Roupas", "roupas", null, List.of(brandRoot));

    String rootResJson = mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(rootReq)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String rootId = objectMapper.readTree(rootResJson).get("id").asText();

    AttributeDefinition brandSub = new AttributeDefinition("brand", "Different Brand", AttributeDefinition.AttributeType.STRING, null, true, true, List.of());
    CreateCategoryRequest subReq = new CreateCategoryRequest("Camisetas", "camisetas", rootId, List.of(brandSub));

    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(subReq)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.failureCode", is("INVALID_ATTRIBUTE_DEFINITION")))
        .andExpect(jsonPath("$.correlationId", notNullValue()));
  }

  @Test
  @DisplayName("Should return 422 ProblemDetail when attribute definition has null type")
  void shouldReturn422_whenAttributeDefinitionHasNullType() throws Exception {
    AttributeDefinition invalidAttr = new AttributeDefinition("color", "Color", null, null, false, true, List.of());
    CreateCategoryRequest req = new CreateCategoryRequest("Calçados", "calcados", null, List.of(invalidAttr));

    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.failureCode", is("INVALID_ATTRIBUTE_DEFINITION")));
  }

  @Test
  @DisplayName("Should return 422 ProblemDetail when attribute definition has blank label")
  void shouldReturn422_whenAttributeDefinitionHasBlankLabel() throws Exception {
    AttributeDefinition invalidAttr = new AttributeDefinition("color", "  ", AttributeDefinition.AttributeType.STRING, null, false, true, List.of());
    CreateCategoryRequest req = new CreateCategoryRequest("Calçados", "calcados", null, List.of(invalidAttr));

    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.failureCode", is("INVALID_ATTRIBUTE_DEFINITION")));
  }

  @Test
  @DisplayName("Should return 422 ProblemDetail when attribute definition has blank key")
  void shouldReturn422_whenAttributeDefinitionHasBlankKey() throws Exception {
    AttributeDefinition invalidAttr = new AttributeDefinition("", "Color", AttributeDefinition.AttributeType.STRING, null, false, true, List.of());
    CreateCategoryRequest req = new CreateCategoryRequest("Calçados", "calcados", null, List.of(invalidAttr));

    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.failureCode", is("INVALID_ATTRIBUTE_DEFINITION")));
  }

  @Test
  @DisplayName("Should return 422 ProblemDetail when ENUM attribute has no allowed values")
  void shouldReturn422_whenEnumAttributeHasNoAllowedValues() throws Exception {
    AttributeDefinition invalidAttr = new AttributeDefinition("size", "Size", AttributeDefinition.AttributeType.ENUM, null, false, true, List.of());
    CreateCategoryRequest req = new CreateCategoryRequest("Calçados", "calcados", null, List.of(invalidAttr));

    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.failureCode", is("INVALID_ATTRIBUTE_DEFINITION")));
  }

  @Test
  @DisplayName("Should return 409 ProblemDetail when slug already exists")
  void shouldReturn409_whenSlugAlreadyExists() throws Exception {
    CreateCategoryRequest req = new CreateCategoryRequest("Eletrônicos", "eletronicos", null, List.of());

    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.failureCode", is("DUPLICATE_SLUG")));
  }

  @Test
  @DisplayName("Should return 409 ProblemDetail when deleting category that has children")
  void shouldReturn409_whenDeletingCategoryWithChildren() throws Exception {
    CreateCategoryRequest rootReq = new CreateCategoryRequest("Casa", "casa", null, List.of());
    String rootRes = mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(rootReq)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String rootId = objectMapper.readTree(rootRes).get("id").asText();

    CreateCategoryRequest subReq = new CreateCategoryRequest("Moveis", "moveis", rootId, List.of());
    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(subReq)))
        .andExpect(status().isCreated());

    mockMvc.perform(delete("/api/v1/categories/" + rootId)
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.failureCode", is("CATEGORY_NOT_EMPTY")));
  }

  @Test
  @DisplayName("Should return 403 Forbidden when USER role tries to mutate category")
  void shouldReturn403_whenUserRoleTriesToMutateCategory() throws Exception {
    CreateCategoryRequest req = new CreateCategoryRequest("Proibido", "proibido", null, List.of());

    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.failureCode", is("FORBIDDEN")));

    mockMvc.perform(delete("/api/v1/categories/cat-123")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.failureCode", is("FORBIDDEN")));
  }
}
