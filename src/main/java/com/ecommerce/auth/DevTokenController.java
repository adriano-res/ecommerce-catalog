package com.ecommerce.auth;

import com.ecommerce.auth.dto.DevTokenRequest;
import com.ecommerce.auth.dto.DevTokenResponse;
import com.ecommerce.shared.enums.UserRole;
import com.ecommerce.shared.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!prod")
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Development token generation for role-based access")
public class DevTokenController {

  private final JwtService jwtService;

  public DevTokenController(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Operation(
      summary = "Generate development JWT token",
      description =
          "Generates a stateless JWT bearer token with ROLE_ADMIN or ROLE_USER for testing secured"
              + " endpoints in development environments.")
  @ApiResponse(
      responseCode = "200",
      description = "Token generated successfully",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = DevTokenResponse.class)))
  @PostMapping("/dev-token")
  public ResponseEntity<DevTokenResponse> generateDevToken(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Role and username configuration. Defaults to ADMIN/admin if omitted.",
              required = false)
          @RequestBody(required = false)
          DevTokenRequest request) {
    DevTokenRequest safeRequest =
        (request != null) ? request : new DevTokenRequest("ADMIN", "admin");

    UserRole role = UserRole.fromString(safeRequest.resolvedRole());
    String username = safeRequest.resolvedUsername();

    String token = jwtService.generateToken(username, role);

    DevTokenResponse response =
        new DevTokenResponse(
            token, "Bearer", jwtService.getExpirationSeconds(), role.getAuthority(), username);

    return ResponseEntity.ok(response);
  }
}
