package com.ecommerce.openapi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class OpenApiIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("Should return OpenAPI specification when querying api-docs endpoint")
  void shouldReturnOpenApiSpecification_whenQueryingApiDocs() throws Exception {
    mockMvc.perform(get("/swagger-ui/api-docs").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.openapi", notNullValue()))
        .andExpect(jsonPath("$.info.title", is("E-Commerce Catalog API")))
        .andExpect(jsonPath("$.info.version", is("1.0.0")))
        .andExpect(jsonPath("$.paths['/api/v1/products']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/categories']").exists())
        .andExpect(jsonPath("$.components.securitySchemes.BearerAuth").exists())
        .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.type", is("http")))
        .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.scheme", is("bearer")))
        .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.bearerFormat", is("JWT")));
  }

  @Test
  @DisplayName("Should return Swagger UI index page when accessed without authentication")
  void shouldReturnSwaggerUiIndex_whenAccessedWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/swagger-ui/index.html"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Swagger UI")));
  }
}
