package com.ecommerce.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.category.CategoryRepository;
import com.ecommerce.category.dto.AttributeDefinition;
import com.ecommerce.category.dto.AttributeDefinition.AttributeType;
import com.ecommerce.category.entity.CategoryDocument;
import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.UpdateProductRequest;
import com.ecommerce.product.entity.ProductDocument;
import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.shared.audit.AuditMetadata;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ProductServiceIT extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ProductRepository productRepository;

  @Autowired private CategoryRepository categoryRepository;

  @Autowired private JwtService jwtService;

  @Autowired private ObjectMapper objectMapper;

  private String adminToken;
  private CategoryDocument testCategory;

  @BeforeEach
  void setUp() {
    productRepository.deleteAll();
    categoryRepository.deleteAll();

    adminToken = jwtService.generateToken("admin-tester", UserRole.ADMIN);

    AttributeDefinition ram =
        new AttributeDefinition(
            "ram", "RAM", AttributeType.ENUM, "GB", true, true, List.of("4GB", "8GB", "16GB"));
    AttributeDefinition storage =
        new AttributeDefinition(
            "storageGb", "Storage", AttributeType.NUMBER, "GB", true, true, List.of());
    AttributeDefinition is5g =
        new AttributeDefinition(
            "is5g", "5G Support", AttributeType.BOOLEAN, null, false, true, List.of());

    AuditMetadata audit = AuditMetadata.create("admin-tester");
    CategoryDocument rootCat =
        new CategoryDocument(
            null,
            "smartphones",
            "Smartphones",
            null,
            List.of(),
            List.of(ram, storage, is5g),
            audit);
    testCategory = categoryRepository.save(rootCat);
  }

  @Test
  @DisplayName("Should create product in DRAFT status when valid request is submitted")
  void shouldCreateProductInDraftStatus_whenValidRequest() throws Exception {
    CreateProductRequest request =
        new CreateProductRequest(
            "SMART-GX24-8-256",
            "galaxy-x24-8gb",
            "Galaxy X24 8GB 256GB",
            "Samsung",
            "High-end smartphone",
            testCategory.getId(),
            Money.brl("3499.90"),
            Map.of("ram", "8GB", "storageGb", 256, "is5g", true),
            List.of(new ProductImage("https://cdn.loja.com/gx24.jpg", "Front view")));

    String responseJson =
        mockMvc
            .perform(
                post("/api/v1/products")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("ETag"))
            .andExpect(jsonPath("$.sku", is("SMART-GX24-8-256")))
            .andExpect(jsonPath("$.slug", is("galaxy-x24-8gb")))
            .andExpect(jsonPath("$.status", is("DRAFT")))
            .andExpect(jsonPath("$.price.amount", is("3499.90")))
            .andExpect(jsonPath("$.price.currency", is("BRL")))
            .andExpect(jsonPath("$.attributes.ram", is("8GB")))
            .andExpect(jsonPath("$.attributes.storageGb", is(256)))
            .andExpect(jsonPath("$.attributes.is5g", is(true)))
            .andExpect(jsonPath("$.version", is(0)))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String productId = objectMapper.readTree(responseJson).get("id").asText();
    ProductDocument doc = productRepository.findById(productId).orElseThrow();
    assertThat(doc.getStatus()).isEqualTo(ProductStatus.DRAFT);
    assertThat(doc.getCategoryPath()).containsExactly(testCategory.getId());
    assertThat(doc.getAttributes()).hasSize(3);
  }

  @Test
  @DisplayName("Should return 400 Bad Request when price format is invalid scalar instead of object")
  void shouldReturn400BadRequest_whenPriceFormatIsInvalidScalar() throws Exception {
    String rawJson =
        """
        {
          "sku": "MISS-MISER-452",
          "name": "Miss the Misery",
          "brand": "Sony Music",
          "description": "Album de heavy metal",
          "categoryId": "%s",
          "price": 8500
        }
        """
            .formatted(testCategory.getId());

    mockMvc
        .perform(
            post("/api/v1/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.failureCode", is("VALIDATION_ERROR")));
  }

  @Test
  @DisplayName("Should reject product creation when attribute validation fails")
  void shouldRejectProductCreation_whenAttributeValidationFails() throws Exception {
    CreateProductRequest invalidRequest =
        new CreateProductRequest(
            "SMART-INVALID",
            null,
            "Invalid Phone",
            "Generic",
            "Desc",
            testCategory.getId(),
            Money.brl("999.00"),
            Map.of("ram", "8GB", "unknownAttr", "value"), // missing storageGb, undeclared unknownAttr
            List.of());

    mockMvc
        .perform(
            post("/api/v1/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.failureCode", is("INVALID_PRODUCT_ATTRIBUTES")));
  }

  @Test
  @DisplayName("Should reject product creation when duplicate SKU is used")
  void shouldRejectProductCreation_whenDuplicateSkuIsUsed() throws Exception {
    CreateProductRequest request =
        new CreateProductRequest(
            "SMART-DUP",
            "phone-one",
            "Phone One",
            "Brand",
            "Desc",
            testCategory.getId(),
            Money.brl("1000.00"),
            Map.of("ram", "8GB", "storageGb", 128),
            List.of());

    mockMvc
        .perform(
            post("/api/v1/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    CreateProductRequest duplicateSkuRequest =
        new CreateProductRequest(
            "SMART-DUP",
            "phone-two",
            "Phone Two",
            "Brand",
            "Desc",
            testCategory.getId(),
            Money.brl("1200.00"),
            Map.of("ram", "8GB", "storageGb", 128),
            List.of());

    mockMvc
        .perform(
            post("/api/v1/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateSkuRequest)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.failureCode", is("DUPLICATE_SKU")));
  }

  @Test
  @DisplayName("Should update product successfully when If-Match matches document version")
  void shouldUpdateProductSuccessfully_whenIfMatchMatchesVersion() throws Exception {
    CreateProductRequest createReq =
        new CreateProductRequest(
            "SMART-UPDATE-1",
            "phone-update",
            "Original Name",
            "Original Brand",
            "Original Desc",
            testCategory.getId(),
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

    String id = objectMapper.readTree(createRes).get("id").asText();

    UpdateProductRequest updateReq =
        new UpdateProductRequest(
            "Updated Name",
            "Updated Brand",
            "Updated Desc",
            null,
            Money.brl("1800.00"),
            Map.of("ram", "16GB", "storageGb", 256),
            null);

    mockMvc
        .perform(
            patch("/api/v1/products/" + id)
                .header("Authorization", "Bearer " + adminToken)
                .header("If-Match", "\"0\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Updated Name")))
        .andExpect(jsonPath("$.brand", is("Updated Brand")))
        .andExpect(jsonPath("$.price.amount", is("1800.00")))
        .andExpect(jsonPath("$.attributes.ram", is("16GB")))
        .andExpect(jsonPath("$.attributes.storageGb", is(256)))
        .andExpect(jsonPath("$.version", is(1)));

    ProductDocument updatedDoc = productRepository.findById(id).orElseThrow();
    assertThat(updatedDoc.getVersion()).isEqualTo(1L);
    assertThat(updatedDoc.getName()).isEqualTo("Updated Name");
  }

  @Test
  @DisplayName("Should publish product and transition status from DRAFT to ACTIVE")
  void shouldPublishProductAndChangeStatusToActive_whenDraftProduct() throws Exception {
    CreateProductRequest createReq =
        new CreateProductRequest(
            "SMART-PUB-1",
            "phone-publish",
            "Publish Phone",
            "Brand",
            "Desc",
            testCategory.getId(),
            Money.brl("2000.00"),
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

    String id = objectMapper.readTree(createRes).get("id").asText();

    mockMvc
        .perform(
            post("/api/v1/products/" + id + "/publish")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("ACTIVE")));

    ProductDocument published = productRepository.findById(id).orElseThrow();
    assertThat(published.getStatus()).isEqualTo(ProductStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should archive product and transition status to ARCHIVED")
  void shouldArchiveProductAndChangeStatusToArchived_whenActiveProduct() throws Exception {
    CreateProductRequest createReq =
        new CreateProductRequest(
            "SMART-ARC-1",
            "phone-archive",
            "Archive Phone",
            "Brand",
            "Desc",
            testCategory.getId(),
            Money.brl("2000.00"),
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

    String id = objectMapper.readTree(createRes).get("id").asText();

    mockMvc
        .perform(
            post("/api/v1/products/" + id + "/archive")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("ARCHIVED")));

    ProductDocument archived = productRepository.findById(id).orElseThrow();
    assertThat(archived.getStatus()).isEqualTo(ProductStatus.ARCHIVED);
  }

  @Test
  @DisplayName("Should return 404 when public user requests DRAFT product")
  void shouldReturn404_whenPublicUserRequestsDraftProduct() throws Exception {
    CreateProductRequest createReq =
        new CreateProductRequest(
            "SMART-PUB-HIDDEN",
            "phone-hidden",
            "Hidden Phone",
            "Brand",
            "Desc",
            testCategory.getId(),
            Money.brl("2000.00"),
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

    String id = objectMapper.readTree(createRes).get("id").asText();

    // Public request (no Authorization token)
    mockMvc
        .perform(get("/api/v1/products/" + id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.failureCode", is("PRODUCT_NOT_FOUND")));
  }

  @Test
  @DisplayName("Should return product when public user requests ACTIVE product")
  void shouldReturnProduct_whenPublicUserRequestsActiveProduct() throws Exception {
    CreateProductRequest createReq =
        new CreateProductRequest(
            "SMART-ACTIVE-PUB",
            "phone-active-pub",
            "Public Phone",
            "Brand",
            "Desc",
            testCategory.getId(),
            Money.brl("2500.00"),
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

    String id = objectMapper.readTree(createRes).get("id").asText();

    mockMvc
        .perform(
            post("/api/v1/products/" + id + "/publish")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());

    // Public request (no Authorization header)
    mockMvc
        .perform(get("/api/v1/products/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(id)))
        .andExpect(jsonPath("$.status", is("ACTIVE")));

    // Public request by slug
    mockMvc
        .perform(get("/api/v1/products/phone-active-pub"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(id)))
        .andExpect(jsonPath("$.slug", is("phone-active-pub")));
  }

  @Test
  @DisplayName("Should return draft product when admin user requests DRAFT product")
  void shouldReturnDraftProduct_whenAdminUserRequestsDraftProduct() throws Exception {
    CreateProductRequest createReq =
        new CreateProductRequest(
            "SMART-ADMIN-VIEW",
            "phone-admin-view",
            "Admin Visible Phone",
            "Brand",
            "Desc",
            testCategory.getId(),
            Money.brl("3000.00"),
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

    String id = objectMapper.readTree(createRes).get("id").asText();

    mockMvc
        .perform(
            get("/api/v1/products/" + id).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(id)))
        .andExpect(jsonPath("$.status", is("DRAFT")));
  }
}
