package com.ecommerce.category;

import com.ecommerce.category.dto.CategoryResponse;
import com.ecommerce.category.dto.CategoryTreeResponse;
import com.ecommerce.category.dto.CreateCategoryRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
public class CategoryController {

  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
    CategoryResponse response = categoryService.createCategory(request);
    return ResponseEntity.created(URI.create("/api/v1/categories/" + response.id())).body(response);
  }

  @GetMapping("/tree")
  public ResponseEntity<List<CategoryTreeResponse>> getCategoryTree() {
    return ResponseEntity.ok(categoryService.getCategoryTree());
  }

  @GetMapping("/{idOrSlug}")
  public ResponseEntity<CategoryResponse> getCategoryByIdOrSlug(@PathVariable String idOrSlug) {
    return ResponseEntity.ok(categoryService.getByIdOrSlug(idOrSlug));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteCategory(@PathVariable String id) {
    categoryService.deleteCategory(id);
    return ResponseEntity.noContent().build();
  }
}
