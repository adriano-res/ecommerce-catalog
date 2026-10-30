package com.ecommerce.auth;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.auth.dto.DevTokenRequest;
import com.ecommerce.shared.enums.UserRole;
import com.ecommerce.shared.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DevTokenController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.ecommerce.shared.config.WebMvcConfig.class)
class DevTokenControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtService jwtService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("Should return 200 with ADMIN token when role is ADMIN")
  void shouldReturnAdminToken_whenAdminRoleRequested() throws Exception {
    when(jwtService.generateToken(eq("carlos"), eq(UserRole.ADMIN))).thenReturn("mock-admin-token");
    when(jwtService.getExpirationSeconds()).thenReturn(86400L);

    DevTokenRequest request = new DevTokenRequest("ADMIN", "carlos");

    mockMvc.perform(post("/api/v1/auth/dev-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", is("mock-admin-token")))
        .andExpect(jsonPath("$.tokenType", is("Bearer")))
        .andExpect(jsonPath("$.expiresIn", is(86400)))
        .andExpect(jsonPath("$.role", is("ROLE_ADMIN")))
        .andExpect(jsonPath("$.username", is("carlos")));
  }

  @Test
  @DisplayName("Should return 200 with USER token when role is USER")
  void shouldReturnUserToken_whenUserRoleRequested() throws Exception {
    when(jwtService.generateToken(eq("maria"), eq(UserRole.USER))).thenReturn("mock-user-token");
    when(jwtService.getExpirationSeconds()).thenReturn(86400L);

    DevTokenRequest request = new DevTokenRequest("USER", "maria");

    mockMvc.perform(post("/api/v1/auth/dev-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", is("mock-user-token")))
        .andExpect(jsonPath("$.tokenType", is("Bearer")))
        .andExpect(jsonPath("$.expiresIn", is(86400)))
        .andExpect(jsonPath("$.role", is("ROLE_USER")))
        .andExpect(jsonPath("$.username", is("maria")));
  }

  @Test
  @DisplayName("Should default to ADMIN role and admin username when empty payload provided")
  void shouldDefaultToAdmin_whenPayloadIsEmpty() throws Exception {
    when(jwtService.generateToken(eq("admin"), eq(UserRole.ADMIN))).thenReturn("default-admin-token");
    when(jwtService.getExpirationSeconds()).thenReturn(86400L);

    mockMvc.perform(post("/api/v1/auth/dev-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", is("default-admin-token")))
        .andExpect(jsonPath("$.role", is("ROLE_ADMIN")))
        .andExpect(jsonPath("$.username", is("admin")));
  }
}
