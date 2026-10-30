package com.ecommerce.shared.security;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TestSecurityController {

  @GetMapping("/products")
  public ResponseEntity<Map<String, String>> publicProducts() {
    return ResponseEntity.ok(Map.of("message", "public products list"));
  }

  @GetMapping("/products/test-product")
  public ResponseEntity<Map<String, String>> publicProduct() {
    return ResponseEntity.ok(Map.of("message", "public product data"));
  }

  @GetMapping("/categories")
  public ResponseEntity<Map<String, String>> publicCategories() {
    return ResponseEntity.ok(Map.of("message", "public categories tree"));
  }

  @PostMapping("/categories")
  public ResponseEntity<Map<String, String>> createCategory() {
    return ResponseEntity.ok(Map.of("message", "admin created root category"));
  }

  @PostMapping("/categories/test-category")
  public ResponseEntity<Map<String, String>> adminCategory() {
    return ResponseEntity.ok(Map.of("message", "admin created category"));
  }

  @PostMapping("/products")
  public ResponseEntity<Map<String, String>> createProduct() {
    return ResponseEntity.ok(Map.of("message", "admin created root product"));
  }

  @GetMapping("/custom-protected/test")
  public ResponseEntity<Map<String, String>> protectedResource() {
    return ResponseEntity.ok(Map.of("message", "authenticated data"));
  }
}
