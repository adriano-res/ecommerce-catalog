package com.ecommerce.shared.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.category.CategoryRepository;
import com.ecommerce.category.dto.CreateCategoryRequest;
import com.ecommerce.category.entity.CategoryDocument;
import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.entity.ProductDocument;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.shared.enums.UserRole;
import com.ecommerce.shared.money.Money;
import com.ecommerce.shared.security.JwtService;
import com.ecommerce.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class AuditMetadataIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private JwtService jwtService;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void cleanCollections() {
    productRepository.deleteAll();
    categoryRepository.deleteAll();
  }

  @Test
  @DisplayName("Should populate audit fields with authenticated JWT username when category is created")
  void shouldPopulateAuditFieldsWithJwtUsername_whenCategoryIsCreated() throws Exception {
    String creatorUsername = "admin:carlos_silva";
    String token = jwtService.generateToken(creatorUsername, UserRole.ADMIN);

    CreateCategoryRequest request = new CreateCategoryRequest(
        "Electronics",
        "electronics",
        null,
        Collections.emptyList());

    MvcResult result = mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn();

    JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
    String categoryId = responseNode.get("id").asText();

    CategoryDocument savedCategory = categoryRepository.findById(categoryId).orElseThrow();
    assertThat(savedCategory.getAudit()).isNotNull();
    assertThat(savedCategory.getAudit().createdBy()).isEqualTo(creatorUsername);
    assertThat(savedCategory.getAudit().updatedBy()).isEqualTo(creatorUsername);
    assertThat(savedCategory.getAudit().createdAt()).isNotNull();
    assertThat(savedCategory.getAudit().updatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should populate audit fields with JWT username on product creation and update")
  void shouldPopulateAuditFieldsWithJwtUsername_whenProductIsCreatedAndUpdated() throws Exception {
    String creatorUsername = "admin:ana_lima";
    String creatorToken = jwtService.generateToken(creatorUsername, UserRole.ADMIN);

    CreateCategoryRequest catRequest = new CreateCategoryRequest(
        "Audio",
        "audio",
        null,
        Collections.emptyList());

    MvcResult catResult = mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + creatorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(catRequest)))
        .andExpect(status().isCreated())
        .andReturn();

    String categoryId = objectMapper.readTree(catResult.getResponse().getContentAsString()).get("id").asText();

    CreateProductRequest prodRequest = new CreateProductRequest(
        "AUD-HEADPHONE-01",
        "wireless-headphones",
        "Wireless Headphones",
        "Sony",
        "Noise cancelling headphones",
        categoryId,
        Money.brl("1299.90"),
        Map.of(),
        List.of());

    MvcResult prodResult = mockMvc.perform(post("/api/v1/products")
            .header("Authorization", "Bearer " + creatorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(prodRequest)))
        .andExpect(status().isCreated())
        .andReturn();

    String productId = objectMapper.readTree(prodResult.getResponse().getContentAsString()).get("id").asText();

    ProductDocument createdProduct = productRepository.findById(productId).orElseThrow();
    assertThat(createdProduct.getAudit()).isNotNull();
    assertThat(createdProduct.getAudit().createdBy()).isEqualTo(creatorUsername);
    assertThat(createdProduct.getAudit().updatedBy()).isEqualTo(creatorUsername);

    String updaterUsername = "admin:marcos_souza";
    String updaterToken = jwtService.generateToken(updaterUsername, UserRole.ADMIN);

    mockMvc.perform(patch("/api/v1/products/" + productId + "/publish")
            .header("Authorization", "Bearer " + updaterToken))
        .andExpect(status().isOk());

    ProductDocument updatedProduct = productRepository.findById(productId).orElseThrow();
    assertThat(updatedProduct.getAudit()).isNotNull();
    assertThat(updatedProduct.getAudit().createdBy()).isEqualTo(creatorUsername);
    assertThat(updatedProduct.getAudit().updatedBy()).isEqualTo(updaterUsername);
    assertThat(updatedProduct.getAudit().updatedAt()).isAfterOrEqualTo(createdProduct.getAudit().createdAt());
  }
}
