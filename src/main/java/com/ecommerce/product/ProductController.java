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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
@Tag(
    name = "Products",
    description = "Product lifecycle management, image uploads, and faceted search")
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @Operation(
      summary = "Create product",
      description =
          "Creates a new product in DRAFT status with validated dynamic attributes matching"
              + " category rules. Requires ROLE_ADMIN.")
  @ApiResponse(
      responseCode = "201",
      description = "Product created successfully",
      headers =
          @Header(
              name = "ETag",
              description = "Version ETag for optimistic concurrency control",
              schema = @Schema(type = "string")),
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ProductResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Validation error",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - authentication required",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden - requires ROLE_ADMIN authority",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Conflict - SKU or slug already exists",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "422",
      description = "Unprocessable Entity - invalid attributes according to category rules",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProductResponse> createProduct(
      @Valid @RequestBody CreateProductRequest request) {
    ProductResponse response = productService.createProduct(request);
    return ResponseEntity.created(URI.create("/api/v1/products/" + response.id()))
        .eTag("\"" + response.version() + "\"")
        .body(response);
  }

  @Operation(
      summary = "Partially update product",
      description =
          "Updates product fields with optimistic concurrency control via If-Match header."
              + " Requires ROLE_ADMIN.")
  @ApiResponse(
      responseCode = "200",
      description = "Product updated successfully",
      headers =
          @Header(
              name = "ETag",
              description = "Updated version ETag",
              schema = @Schema(type = "string")),
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ProductResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Missing or invalid If-Match header / Validation error",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - authentication required",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden - requires ROLE_ADMIN authority",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Product not found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Optimistic locking failure / concurrent modification conflict",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "422",
      description = "Unprocessable Entity - invalid attributes according to category rules",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProductResponse> updateProduct(
      @Parameter(
              name = "id",
              description = "Product ID to update",
              example = "6720f1a2c3d4e5f600000001")
          @PathVariable
          String id,
      @Parameter(
              name = "If-Match",
              in = ParameterIn.HEADER,
              required = true,
              description = "Expected product version ETag (e.g. \\\"0\\\" or 0)",
              example = "\"0\"")
          @RequestHeader(value = "If-Match", required = true)
          String ifMatch,
      @Valid @RequestBody UpdateProductRequest request) {
    Long expectedVersion = parseIfMatch(ifMatch);
    ProductResponse response = productService.updateProduct(id, expectedVersion, request);
    return ResponseEntity.ok().eTag("\"" + response.version() + "\"").body(response);
  }

  @Operation(
      summary = "Publish product",
      description =
          "Transitions product from DRAFT to ACTIVE status, invalidating cache and publishing"
              + " ProductChanged event. Requires ROLE_ADMIN.")
  @ApiResponse(
      responseCode = "200",
      description = "Product published successfully",
      headers =
          @Header(
              name = "ETag",
              description = "Updated version ETag",
              schema = @Schema(type = "string")),
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ProductResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - authentication required",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden - requires ROLE_ADMIN authority",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Product not found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Conflict - product is not in DRAFT status",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping("/{id}/publish")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProductResponse> publishProduct(
      @Parameter(
              name = "id",
              description = "Product ID to publish",
              example = "6720f1a2c3d4e5f600000001")
          @PathVariable
          String id) {
    ProductResponse response = productService.publishProduct(id);
    return ResponseEntity.ok().eTag("\"" + response.version() + "\"").body(response);
  }

  @Operation(
      summary = "Archive product",
      description =
          "Transitions product from ACTIVE to ARCHIVED status, removing from public catalog and"
              + " invalidating cache. Requires ROLE_ADMIN.")
  @ApiResponse(
      responseCode = "200",
      description = "Product archived successfully",
      headers =
          @Header(
              name = "ETag",
              description = "Updated version ETag",
              schema = @Schema(type = "string")),
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ProductResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - authentication required",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden - requires ROLE_ADMIN authority",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Product not found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Conflict - product is not in ACTIVE status",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping("/{id}/archive")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProductResponse> archiveProduct(
      @Parameter(
              name = "id",
              description = "Product ID to archive",
              example = "6720f1a2c3d4e5f600000001")
          @PathVariable
          String id) {
    ProductResponse response = productService.archiveProduct(id);
    return ResponseEntity.ok().eTag("\"" + response.version() + "\"").body(response);
  }

  @Operation(
      summary = "Upload product image",
      description =
          "Uploads a multipart image file to S3 storage and associates it with the product."
              + " Requires ROLE_ADMIN.")
  @ApiResponse(
      responseCode = "200",
      description = "Image uploaded and associated successfully",
      headers =
          @Header(
              name = "ETag",
              description = "Updated version ETag",
              schema = @Schema(type = "string")),
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ProductResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Invalid file or empty payload",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - authentication required",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden - requires ROLE_ADMIN authority",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Product not found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProductResponse> addImage(
      @Parameter(
              name = "id",
              description = "Product ID",
              example = "6720f1a2c3d4e5f600000001")
          @PathVariable
          String id,
      @Parameter(description = "Image binary file (multipart/form-data)")
          @RequestParam("file")
          MultipartFile file,
      @Parameter(
              description = "Optional descriptive alternative text for accessibility",
              example = "Front view")
          @RequestParam(value = "alt", required = false)
          String alt) {
    ProductResponse response = productService.addImage(id, file, alt);
    return ResponseEntity.ok().eTag("\"" + response.version() + "\"").body(response);
  }

  @Operation(
      summary = "Remove product image",
      description =
          "Removes an image from the product and deletes it from S3 storage. Requires ROLE_ADMIN.")
  @ApiResponse(
      responseCode = "200",
      description = "Image removed successfully",
      headers =
          @Header(
              name = "ETag",
              description = "Updated version ETag",
              schema = @Schema(type = "string")),
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ProductResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - authentication required",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden - requires ROLE_ADMIN authority",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Product or image not found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @DeleteMapping("/{id}/images/{imageId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProductResponse> removeImage(
      @Parameter(
              name = "id",
              description = "Product ID",
              example = "6720f1a2c3d4e5f600000001")
          @PathVariable
          String id,
      @Parameter(
              name = "imageId",
              description = "Image UUID to remove",
              example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
          @PathVariable("imageId")
          String imageId) {
    ProductResponse response = productService.removeImage(id, imageId);
    return ResponseEntity.ok().eTag("\"" + response.version() + "\"").body(response);
  }

  @Operation(
      summary = "Search products with dynamic facets",
      description =
          "Faceted search supporting text queries, hierarchical category filtering, dynamic"
              + " attribute filters (attr.<key>=<val>), price ranges, sorting, and cursor"
              + " pagination via a single MongoDB $facet aggregation pipeline.")
  @Parameters({
    @Parameter(
        name = "q",
        in = ParameterIn.QUERY,
        description = "Free-text search query across name, brand, and description",
        example = "Samsung Galaxy"),
    @Parameter(
        name = "category",
        in = ParameterIn.QUERY,
        description = "Category slug for hierarchical subtree filtering",
        example = "smartphones"),
    @Parameter(
        name = "priceMin",
        in = ParameterIn.QUERY,
        description = "Minimum price filter (inclusive)",
        example = "100.00"),
    @Parameter(
        name = "priceMax",
        in = ParameterIn.QUERY,
        description = "Maximum price filter (inclusive)",
        example = "4000.00"),
    @Parameter(
        name = "sort",
        in = ParameterIn.QUERY,
        description = "Sort criteria: relevance, priceAsc, priceDesc, newest",
        schema = @Schema(implementation = SearchSort.class),
        example = "newest"),
    @Parameter(
        name = "cursor",
        in = ParameterIn.QUERY,
        description = "Opaque Base64URL cursor for deterministic forward pagination",
        example = "ZXlKcElqb3pNVEk1T1RBc0ltbG1JanBv..."),
    @Parameter(
        name = "limit",
        in = ParameterIn.QUERY,
        description = "Maximum items per page (1 to 50, default 20)",
        example = "20"),
    @Parameter(
        name = "attr.<key>",
        in = ParameterIn.QUERY,
        description =
            "Dynamic attribute filters by key. Repeatable for OR logic across values, e.g."
                + " attr.ram=8GB&attr.ram=16GB",
        example = "attr.ram=8GB")
  })
  @ApiResponse(
      responseCode = "200",
      description = "Faceted search results with items, facet counts, and next cursor",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ProductSearchResult.class)))
  @GetMapping("/search")
  public ResponseEntity<ProductSearchResult> search(
      @Parameter(hidden = true) @RequestParam MultiValueMap<String, String> allParams) {
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

  @Operation(
      summary = "Get product by ID or slug",
      description =
          "Retrieves full product details by ID or slug. Public users can only see ACTIVE"
              + " products. Authenticated admins can inspect products in any status. Served from"
              + " Redis Cache-Aside when available.")
  @ApiResponse(
      responseCode = "200",
      description = "Product found",
      headers =
          @Header(
              name = "ETag",
              description = "Product version ETag",
              schema = @Schema(type = "string")),
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ProductResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Product not found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @GetMapping("/{idOrSlug}")
  public ResponseEntity<ProductResponse> getProductByIdOrSlug(
      @Parameter(
              name = "idOrSlug",
              description = "Product ID or unique slug",
              example = "galaxy-x24-8gb-256gb-preto")
          @PathVariable
          String idOrSlug) {
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
