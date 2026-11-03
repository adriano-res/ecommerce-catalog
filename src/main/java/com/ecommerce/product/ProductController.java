package com.ecommerce.product;

import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.UpdateProductRequest;
import com.ecommerce.shared.error.DomainException;
import com.ecommerce.shared.error.FailureCode;
import com.ecommerce.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** REST controller exposing product catalog lifecycle and query operations. */
@RestController
@RequestMapping("/products")
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProductResponse> createProduct(
      @Valid @RequestBody CreateProductRequest request) {
    ProductResponse response = productService.createProduct(request);
    return ResponseEntity.created(URI.create("/api/v1/products/" + response.id()))
        .eTag("\"" + response.version() + "\"")
        .body(response);
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProductResponse> updateProduct(
      @PathVariable String id,
      @RequestHeader(value = "If-Match", required = true) String ifMatch,
      @Valid @RequestBody UpdateProductRequest request) {
    Long expectedVersion = parseIfMatch(ifMatch);
    ProductResponse response = productService.updateProduct(id, expectedVersion, request);
    return ResponseEntity.ok().eTag("\"" + response.version() + "\"").body(response);
  }

  @PostMapping("/{id}/publish")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProductResponse> publishProduct(@PathVariable String id) {
    ProductResponse response = productService.publishProduct(id);
    return ResponseEntity.ok().eTag("\"" + response.version() + "\"").body(response);
  }

  @PostMapping("/{id}/archive")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProductResponse> archiveProduct(@PathVariable String id) {
    ProductResponse response = productService.archiveProduct(id);
    return ResponseEntity.ok().eTag("\"" + response.version() + "\"").body(response);
  }

  @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProductResponse> addImage(
      @PathVariable String id,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "alt", required = false) String alt) {
    ProductResponse response = productService.addImage(id, file, alt);
    return ResponseEntity.ok().eTag("\"" + response.version() + "\"").body(response);
  }

  @DeleteMapping("/{id}/images/{imageId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProductResponse> removeImage(
      @PathVariable String id,
      @PathVariable("imageId") String imageId) {
    ProductResponse response = productService.removeImage(id, imageId);
    return ResponseEntity.ok().eTag("\"" + response.version() + "\"").body(response);
  }

  @GetMapping("/{idOrSlug}")
  public ResponseEntity<ProductResponse> getProductByIdOrSlug(@PathVariable String idOrSlug) {
    boolean publicOnly = !SecurityUtils.isAdmin();
    ProductResponse response = productService.getByIdOrSlug(idOrSlug, publicOnly);
    return ResponseEntity.ok().eTag("\"" + response.version() + "\"").body(response);
  }

  private Long parseIfMatch(String ifMatch) {
    if (ifMatch == null || ifMatch.isBlank()) {
      throw new DomainException(FailureCode.VALIDATION_ERROR, "If-Match header is required");
    }
    String clean = ifMatch.replace("\"", "").replace("W/", "").trim();
    try {
      return Long.parseLong(clean);
    } catch (NumberFormatException e) {
      throw new DomainException(
          FailureCode.VALIDATION_ERROR, "Invalid If-Match header value: " + ifMatch);
    }
  }
}
