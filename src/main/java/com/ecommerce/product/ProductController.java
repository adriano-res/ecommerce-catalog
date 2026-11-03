package com.ecommerce.product;

import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductSearchResult;
import com.ecommerce.product.dto.SearchFilterRequest;
import com.ecommerce.product.dto.SearchSort;
import com.ecommerce.product.dto.UpdateProductRequest;
import com.ecommerce.shared.error.DomainException;
import com.ecommerce.shared.error.FailureCode;
import com.ecommerce.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
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

  @GetMapping("/search")
  public ResponseEntity<ProductSearchResult> search(
      @RequestParam MultiValueMap<String, String> allParams) {
    String q = allParams.getFirst("q");
    String category = allParams.getFirst("category");
    String cursor = allParams.getFirst("cursor");
    String limitParam = allParams.getFirst("limit");
    String sortParam = allParams.getFirst("sort");
    String priceMinParam = allParams.getFirst("priceMin");
    String priceMaxParam = allParams.getFirst("priceMax");

    Integer limit = null;
    if (limitParam != null && !limitParam.isBlank()) {
      try {
        limit = Integer.parseInt(limitParam.trim());
      } catch (NumberFormatException ignored) {
      }
    }

    BigDecimal priceMin = null;
    if (priceMinParam != null && !priceMinParam.isBlank()) {
      try {
        priceMin = new BigDecimal(priceMinParam.trim());
      } catch (NumberFormatException ignored) {
      }
    }

    BigDecimal priceMax = null;
    if (priceMaxParam != null && !priceMaxParam.isBlank()) {
      try {
        priceMax = new BigDecimal(priceMaxParam.trim());
      } catch (NumberFormatException ignored) {
      }
    }

    SearchSort sort = SearchSort.fromString(sortParam);

    Map<String, List<String>> attributes = new LinkedHashMap<>();
    for (Map.Entry<String, List<String>> entry : allParams.entrySet()) {
      String key = entry.getKey();
      if (key != null && key.startsWith("attr.")) {
        String attrKey = key.substring("attr.".length());
        List<String> values = new ArrayList<>();
        for (String rawVal : entry.getValue()) {
          if (rawVal != null) {
            for (String part : rawVal.split(",")) {
              if (!part.isBlank()) {
                values.add(part.trim());
              }
            }
          }
        }
        if (!values.isEmpty()) {
          attributes.put(attrKey, values);
        }
      }
    }

    SearchFilterRequest request =
        new SearchFilterRequest(
            (q != null && !q.isBlank()) ? q.trim() : null,
            (category != null && !category.isBlank()) ? category.trim() : null,
            attributes,
            priceMin,
            priceMax,
            sort,
            (cursor != null && !cursor.isBlank()) ? cursor.trim() : null,
            limit);

    ProductSearchResult result = productService.search(request);
    return ResponseEntity.ok(result);
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
