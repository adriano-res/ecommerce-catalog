package com.ecommerce.category;

import com.ecommerce.category.dto.CategoryResponse;
import com.ecommerce.category.dto.CategoryTreeResponse;
import com.ecommerce.category.dto.CreateCategoryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
@Tag(
    name = "Categories",
    description = "Hierarchical category taxonomy and dynamic attribute definitions")
public class CategoryController {

  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @Operation(
      summary = "Create category",
      description =
          "Creates a new category in the hierarchy with optional parent and attribute definitions."
              + " Requires ROLE_ADMIN.")
  @ApiResponse(
      responseCode = "201",
      description = "Category created successfully",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = CategoryResponse.class)))
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
      description = "Conflict - category slug already exists",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "422",
      description = "Unprocessable Entity - invalid attribute definition",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CategoryResponse> createCategory(
      @Valid @RequestBody CreateCategoryRequest request) {
    CategoryResponse response = categoryService.createCategory(request);
    return ResponseEntity.created(URI.create("/api/v1/categories/" + response.id())).body(response);
  }

  @Operation(
      summary = "Get category tree",
      description = "Retrieves the complete hierarchical tree of categories. Cached in Redis.")
  @ApiResponse(
      responseCode = "200",
      description = "Category tree retrieved successfully",
      content =
          @Content(
              mediaType = "application/json",
              array =
                  @ArraySchema(
                      schema = @Schema(implementation = CategoryTreeResponse.class))))
  @GetMapping("/tree")
  public ResponseEntity<List<CategoryTreeResponse>> getCategoryTree() {
    return ResponseEntity.ok(categoryService.getCategoryTree());
  }

  @Operation(
      summary = "Get category by ID or slug",
      description =
          "Retrieves category details and effective inherited attribute definitions by ID or"
              + " unique slug.")
  @ApiResponse(
      responseCode = "200",
      description = "Category found",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = CategoryResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Category not found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @GetMapping("/{idOrSlug}")
  public ResponseEntity<CategoryResponse> getCategoryByIdOrSlug(
      @Parameter(
              name = "idOrSlug",
              description = "Category ID or unique slug",
              example = "smartphones")
          @PathVariable
          String idOrSlug) {
    return ResponseEntity.ok(categoryService.getByIdOrSlug(idOrSlug));
  }

  @Operation(
      summary = "Delete category",
      description =
          "Deletes a category by ID if it has no child categories or associated products. Requires"
              + " ROLE_ADMIN.")
  @ApiResponse(responseCode = "204", description = "Category deleted successfully")
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
      description = "Category not found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Conflict - category not empty (has subcategories or products)",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteCategory(
      @Parameter(
              name = "id",
              description = "Category ID to delete",
              example = "6720f0000000000000000003")
          @PathVariable
          String id) {
    categoryService.deleteCategory(id);
    return ResponseEntity.noContent().build();
  }
}
