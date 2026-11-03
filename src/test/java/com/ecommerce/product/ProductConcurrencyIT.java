package com.ecommerce.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.category.CategoryRepository;
import com.ecommerce.category.dto.AttributeDefinition;
import com.ecommerce.category.dto.AttributeDefinition.AttributeType;
import com.ecommerce.category.entity.CategoryDocument;
import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.UpdateProductRequest;
import com.ecommerce.product.entity.ProductDocument;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.shared.audit.AuditMetadata;
import com.ecommerce.shared.enums.UserRole;
import com.ecommerce.shared.money.Money;
import com.ecommerce.shared.security.JwtService;
import com.ecommerce.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class ProductConcurrencyIT extends AbstractIntegrationTest {

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

    adminToken = jwtService.generateToken("admin-concurrency", UserRole.ADMIN);

    AttributeDefinition ram =
        new AttributeDefinition(
            "ram", "RAM", AttributeType.STRING, null, true, true, List.of());
    AuditMetadata audit = AuditMetadata.create("admin-concurrency");
    CategoryDocument category =
        new CategoryDocument(
            null, "hardware", "Hardware", null, List.of(), List.of(ram), audit);
    testCategory = categoryRepository.save(category);
  }

  @Test
  @DisplayName(
      "Should result in exactly 1 success and 1 conflict 409 when two concurrent updates send same If-Match")
  void shouldRejectConcurrentModification_whenTwoConcurrentUpdatesSendSameIfMatch()
      throws Exception {
    CreateProductRequest createReq =
        new CreateProductRequest(
            "CONC-SKU-1",
            "conc-prod",
            "Initial Concurrent Product",
            "BrandX",
            "Desc",
            testCategory.getId(),
            Money.brl("1000.00"),
            Map.of("ram", "8GB"),
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

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch startLatch = new CountDownLatch(1);

    List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());
    List<String> responseBodies = Collections.synchronizedList(new ArrayList<>());

    UpdateProductRequest req1 =
        new UpdateProductRequest("Name Updated by Thread 1", null, null, null, null, null, null);
    UpdateProductRequest req2 =
        new UpdateProductRequest("Name Updated by Thread 2", null, null, null, null, null, null);

    Future<?> f1 =
        executor.submit(
            () -> {
              try {
                startLatch.await();
                MvcResult res =
                    mockMvc
                        .perform(
                            patch("/api/v1/products/" + productId)
                                .header("Authorization", "Bearer " + adminToken)
                                .header("If-Match", "\"0\"")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req1)))
                        .andReturn();
                statusCodes.add(res.getResponse().getStatus());
                responseBodies.add(res.getResponse().getContentAsString());
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });

    Future<?> f2 =
        executor.submit(
            () -> {
              try {
                startLatch.await();
                MvcResult res =
                    mockMvc
                        .perform(
                            patch("/api/v1/products/" + productId)
                                .header("Authorization", "Bearer " + adminToken)
                                .header("If-Match", "\"0\"")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req2)))
                        .andReturn();
                statusCodes.add(res.getResponse().getStatus());
                responseBodies.add(res.getResponse().getContentAsString());
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });

    startLatch.countDown();
    f1.get(10, TimeUnit.SECONDS);
    f2.get(10, TimeUnit.SECONDS);
    executor.shutdown();

    assertThat(statusCodes).containsExactlyInAnyOrder(200, 409);

    String conflictBody =
        responseBodies.stream().filter(b -> b.contains("CONCURRENT_MODIFICATION")).findFirst().orElseThrow();
    assertThat(conflictBody).contains("CONCURRENT_MODIFICATION");

    ProductDocument finalDoc = productRepository.findById(productId).orElseThrow();
    assertThat(finalDoc.getVersion()).isEqualTo(1L);

    // Further request with stale If-Match "0" must also be rejected with 409 CONCURRENT_MODIFICATION
    UpdateProductRequest staleReq =
        new UpdateProductRequest("Stale Update", null, null, null, null, null, null);
    mockMvc
        .perform(
            patch("/api/v1/products/" + productId)
                .header("Authorization", "Bearer " + adminToken)
                .header("If-Match", "\"0\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(staleReq)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.failureCode", is("CONCURRENT_MODIFICATION")));
  }
}
