package com.ecommerce.shared.web;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CorrelationIdFilterIT extends AbstractIntegrationTest {

  private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
  private static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("Should generate correlation id when header is missing")
  void shouldGenerateCorrelationId_whenHeaderIsMissing() throws Exception {
    mockMvc.perform(get("/api/v1/products/search"))
        .andExpect(status().isOk())
        .andExpect(header().exists(CORRELATION_ID_HEADER))
        .andExpect(header().string(CORRELATION_ID_HEADER, matchesPattern(UUID_REGEX)));
  }

  @Test
  @DisplayName("Should preserve correlation id when header is provided by client")
  void shouldPreserveCorrelationId_whenHeaderIsProvided() throws Exception {
    String clientTraceId = "custom-trace-id-123456789";

    mockMvc.perform(get("/api/v1/products/search")
            .header(CORRELATION_ID_HEADER, clientTraceId))
        .andExpect(status().isOk())
        .andExpect(header().string(CORRELATION_ID_HEADER, clientTraceId));
  }

  @Test
  @DisplayName("Should propagate correlation id to problem detail when error occurs")
  void shouldPropagateCorrelationId_whenErrorOccurs() throws Exception {
    String clientTraceId = "error-trace-id-987654321";

    mockMvc.perform(get("/api/v1/categories/non-existing-category-id")
            .header(CORRELATION_ID_HEADER, clientTraceId))
        .andExpect(status().isNotFound())
        .andExpect(header().string(CORRELATION_ID_HEADER, clientTraceId))
        .andExpect(jsonPath("$.correlationId", is(clientTraceId)));
  }
}
