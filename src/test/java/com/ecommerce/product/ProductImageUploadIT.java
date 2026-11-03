package com.ecommerce.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.category.CategoryRepository;
import com.ecommerce.category.dto.AttributeDefinition;
import com.ecommerce.category.dto.AttributeDefinition.AttributeType;
import com.ecommerce.category.entity.CategoryDocument;
import com.ecommerce.product.entity.ProductDocument;
import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.shared.audit.AuditMetadata;
import com.ecommerce.shared.enums.UserRole;
import com.ecommerce.shared.money.Money;
import com.ecommerce.outbox.OutboxRepository;
import com.ecommerce.outbox.entity.OutboxDocument;
import com.ecommerce.outbox.entity.OutboxStatus;
import com.ecommerce.outbox.jobs.OutboxProcessor;
import com.ecommerce.shared.security.JwtService;
import com.ecommerce.shared.storage.StorageService;
import com.ecommerce.support.AbstractIntegrationTest;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ProductImageUploadIT extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ProductRepository productRepository;

  @Autowired private CategoryRepository categoryRepository;

  @Autowired private OutboxRepository outboxRepository;

  @Autowired private OutboxProcessor outboxProcessor;

  @Autowired private JwtService jwtService;

  @MockBean private StorageService storageService;

  private String adminToken;
  private String userToken;
  private ProductDocument testProduct;

  @BeforeEach
  void setUp() {
    productRepository.deleteAll();
    categoryRepository.deleteAll();
    outboxRepository.deleteAll();

    adminToken = jwtService.generateToken("admin-tester", UserRole.ADMIN);
    userToken = jwtService.generateToken("user-tester", UserRole.USER);

    CategoryDocument category =
        categoryRepository.save(
            new CategoryDocument(
                null,
                "monitors",
                "Monitors",
                null,
                List.of(),
                List.of(
                    new AttributeDefinition(
                        "resolution",
                        "Resolution",
                        AttributeType.STRING,
                        null,
                        false,
                        true,
                        List.of())),
                AuditMetadata.create("admin-tester")));

    testProduct =
        productRepository.save(
            new ProductDocument(
                null,
                "MON-4K-27",
                "monitor-4k-27",
                "Monitor 4K 27 Inch",
                "IPS 144Hz Monitor",
                "Dell",
                category.getId(),
                List.of(category.getId()),
                ProductStatus.DRAFT,
                Money.brl("2499.00"),
                List.of(),
                new ArrayList<>(),
                1L,
                AuditMetadata.create("admin-tester")));
  }

  @Test
  @DisplayName("Should upload PNG image and return updated product with image details")
  void shouldUploadImageAndAddToProduct_whenValidPngProvided() throws Exception {
    String generatedUrl =
        "http://localhost:8333/ecommerce-catalog/products/"
            + testProduct.getId()
            + "/sample-img.png";
    when(storageService.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
        .thenReturn(generatedUrl);

    MockMultipartFile mockFile =
        new MockMultipartFile("file", "front.png", "image/png", "image-binary-data".getBytes());

    mockMvc
        .perform(
            multipart("/api/v1/products/{id}/images", testProduct.getId())
                .file(mockFile)
                .param("alt", "Monitor Front View")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.images", hasSize(1)))
        .andExpect(jsonPath("$.images[0].id").isNotEmpty())
        .andExpect(jsonPath("$.images[0].url", is(generatedUrl)))
        .andExpect(jsonPath("$.images[0].alt", is("Monitor Front View")));

    ProductDocument updated = productRepository.findById(testProduct.getId()).orElseThrow();
    assertThat(updated.getImages()).hasSize(1);
    assertThat(updated.getImages().get(0).url()).isEqualTo(generatedUrl);
  }

  @Test
  @DisplayName("Should reject image upload with 422 INVALID_PAYLOAD when file format is invalid")
  void shouldRejectImageUploadWith422_whenFileFormatIsInvalid() throws Exception {
    MockMultipartFile pdfFile =
        new MockMultipartFile("file", "manual.pdf", "application/pdf", "pdf-data".getBytes());

    mockMvc
        .perform(
            multipart("/api/v1/products/{id}/images", testProduct.getId())
                .file(pdfFile)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code", is("INVALID_PAYLOAD")));
  }

  @Test
  @DisplayName("Should reject image upload with 403 Forbidden when caller is not admin")
  void shouldRejectImageUploadWith403_whenUserIsNotAdmin() throws Exception {
    MockMultipartFile mockFile =
        new MockMultipartFile("file", "front.png", "image/png", "image-binary-data".getBytes());

    mockMvc
        .perform(
            multipart("/api/v1/products/{id}/images", testProduct.getId())
                .file(mockFile)
                .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should remove image and return updated product when valid image id is provided")
  void shouldRemoveImageSuccessfully_whenValidIdentifierProvided() throws Exception {
    String imageUrl =
        "http://localhost:8333/ecommerce-catalog/products/"
            + testProduct.getId()
            + "/photo-1.png";
    testProduct.setImages(new ArrayList<>(List.of(new ProductImage("img-test-1", imageUrl, "Front"))));
    productRepository.save(testProduct);

    mockMvc
        .perform(
            delete("/api/v1/products/{id}/images/{imageId}", testProduct.getId(), "img-test-1")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.images", hasSize(0)));

    verify(storageService, never()).delete(anyString());

    List<OutboxDocument> events = outboxRepository.findAll();
    assertThat(events).hasSize(1);
    OutboxDocument outboxEvent = events.get(0);
    assertThat(outboxEvent.getEventType()).isEqualTo("DELETE_STORAGE_FILE");
    assertThat(outboxEvent.getPayload())
        .isEqualTo("products/" + testProduct.getId() + "/photo-1.png");
    assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);

    ProductDocument updated = productRepository.findById(testProduct.getId()).orElseThrow();
    assertThat(updated.getImages()).isEmpty();

    outboxProcessor.processPendingEvents();
    verify(storageService).delete("products/" + testProduct.getId() + "/photo-1.png");
    OutboxDocument processedEvent = outboxRepository.findById(outboxEvent.getId()).orElseThrow();
    assertThat(processedEvent.getStatus()).isEqualTo(OutboxStatus.COMPLETED);
  }

  @Test
  @DisplayName("Should return 404 RESOURCE_NOT_FOUND when removing non-existent image")
  void shouldReturn404_whenRemovingNonExistentImage() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/products/{id}/images/{imageId}", testProduct.getId(), "non-existent-id")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
  }
}
