package com.ecommerce.auth;

import com.ecommerce.auth.dto.DevTokenRequest;
import com.ecommerce.auth.dto.DevTokenResponse;
import com.ecommerce.shared.enums.UserRole;
import com.ecommerce.shared.security.JwtService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!prod")
@RestController
@RequestMapping("/auth")
public class DevTokenController {

  private final JwtService jwtService;

  public DevTokenController(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @PostMapping("/dev-token")
  public ResponseEntity<DevTokenResponse> generateDevToken(
      @RequestBody(required = false) DevTokenRequest request) {
    DevTokenRequest safeRequest = (request != null)
        ? request
        : new DevTokenRequest("ADMIN", "admin");

    UserRole role = UserRole.fromString(safeRequest.resolvedRole());
    String username = safeRequest.resolvedUsername();

    String token = jwtService.generateToken(username, role);

    DevTokenResponse response = new DevTokenResponse(
        token,
        "Bearer",
        jwtService.getExpirationSeconds(),
        role.getAuthority(),
        username
    );

    return ResponseEntity.ok(response);
  }
}
