package com.ecommerce.shared.security;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.shared.enums.UserRole;
import com.ecommerce.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@Import(TestSecurityController.class)
class SecurityFilterChainTest extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtService jwtService;

  @Test
  @DisplayName("Should allow public read access without authentication")
  void shouldAllowPublicAccess_whenCallingPublicProductEndpoint() throws Exception {
    mockMvc.perform(get("/api/v1/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", is("public products list")));

    mockMvc.perform(get("/api/v1/products/test-product"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", is("public product data")));

    mockMvc.perform(get("/api/v1/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", is("public categories tree")));
  }

  @Test
  @DisplayName("Should return 401 ProblemDetail when calling protected endpoint without token")
  void shouldReturn401ProblemDetail_whenCallingProtectedEndpointWithoutToken() throws Exception {
    mockMvc.perform(get("/api/v1/custom-protected/test"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status", is(401)))
        .andExpect(jsonPath("$.failureCode", is("UNAUTHORIZED")))
        .andExpect(jsonPath("$.title", is("Unauthorized")))
        .andExpect(jsonPath("$.correlationId", notNullValue()))
        .andExpect(jsonPath("$.timestamp", notNullValue()));
  }

  @Test
  @DisplayName("Should return 401 ProblemDetail when calling protected endpoint with malformed token")
  void shouldReturn401ProblemDetail_whenCallingProtectedEndpointWithMalformedToken() throws Exception {
    mockMvc.perform(get("/api/v1/custom-protected/test")
            .header("Authorization", "Bearer invalid-garbage-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status", is(401)))
        .andExpect(jsonPath("$.failureCode", is("UNAUTHORIZED")))
        .andExpect(jsonPath("$.title", is("Unauthorized")))
        .andExpect(jsonPath("$.correlationId", notNullValue()))
        .andExpect(jsonPath("$.timestamp", notNullValue()));
  }

  @Test
  @DisplayName("Should authenticate successfully when authorization header uses lowercase bearer prefix")
  void shouldAuthenticate_whenAuthorizationHeaderUsesLowercaseBearerPrefix() throws Exception {
    String adminToken = jwtService.generateToken("admin-case", UserRole.ADMIN);

    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", is("admin created root category")));
  }

  @Test
  @DisplayName("Should return 403 ProblemDetail when USER role calls admin endpoint")
  void shouldReturn403ProblemDetail_whenUserRoleCallsAdminEndpoint() throws Exception {
    String userToken = jwtService.generateToken("user1", UserRole.USER);

    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status", is(403)))
        .andExpect(jsonPath("$.failureCode", is("FORBIDDEN")))
        .andExpect(jsonPath("$.title", is("Forbidden")))
        .andExpect(jsonPath("$.correlationId", notNullValue()))
        .andExpect(jsonPath("$.timestamp", notNullValue()));

    mockMvc.perform(post("/api/v1/categories/test-category")
            .header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status", is(403)))
        .andExpect(jsonPath("$.failureCode", is("FORBIDDEN")));

    mockMvc.perform(post("/api/v1/products")
            .header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status", is(403)))
        .andExpect(jsonPath("$.failureCode", is("FORBIDDEN")));
  }

  @Test
  @DisplayName("Should allow access to admin endpoint when ADMIN token provided")
  void shouldAllowAccess_whenAdminTokenUsedOnAdminEndpoint() throws Exception {
    String adminToken = jwtService.generateToken("admin1", UserRole.ADMIN);

    mockMvc.perform(post("/api/v1/categories")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", is("admin created root category")));

    mockMvc.perform(post("/api/v1/categories/test-category")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", is("admin created category")));

    mockMvc.perform(post("/api/v1/products")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", is("admin created root product")));
  }

  @Test
  @DisplayName("Should return 200 on dev token generator endpoint without token")
  void shouldAllowDevTokenGeneration_whenNoAuthenticationProvided() throws Exception {
    mockMvc.perform(post("/api/v1/auth/dev-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"role\":\"ADMIN\",\"username\":\"test-admin\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role", is("ROLE_ADMIN")))
        .andExpect(jsonPath("$.username", is("test-admin")))
        .andExpect(jsonPath("$.accessToken", notNullValue()));
  }

  @Test
  @DisplayName("Should permit unauthenticated access to Swagger UI endpoints")
  void shouldPermitAccess_whenAccessingSwaggerEndpointsWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/swagger-ui/api-docs"))
        .andExpect(status().isOk());

    mockMvc.perform(get("/swagger-ui/index.html"))
        .andExpect(status().isOk());
  }
}
