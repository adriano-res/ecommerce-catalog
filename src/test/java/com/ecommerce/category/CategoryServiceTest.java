package com.ecommerce.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.category.dto.AttributeDefinition;
import com.ecommerce.category.dto.CategoryResponse;
import com.ecommerce.category.dto.CreateCategoryRequest;
import com.ecommerce.shared.cache.CacheService;
import com.ecommerce.shared.error.DomainException;
import com.ecommerce.shared.error.FailureCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;

class CategoryServiceTest {

  private CategoryRepository categoryRepository;
  private CacheService cacheService;
  private AuditorAware<String> auditorAware;
  private CategoryService categoryService;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    categoryRepository = mock(CategoryRepository.class);
    cacheService = mock(CacheService.class);
    auditorAware = (AuditorAware<String>) mock(AuditorAware.class);

    when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("admin-carlos"));

    categoryService = new CategoryService(
        categoryRepository,
        cacheService,
        auditorAware,
        null
    );
  }

  @Test
  @DisplayName("Should create root category with empty ancestors when parentId is null")
  void shouldCreateRootCategory_whenParentIdIsNull() {
    CreateCategoryRequest request = new CreateCategoryRequest(
        "Eletrônicos",
        "eletronicos",
        null,
        List.of(new AttributeDefinition("voltage", "Voltage", AttributeDefinition.AttributeType.STRING, "V", false, true, List.of()))
    );

    when(categoryRepository.existsBySlug("eletronicos")).thenReturn(false);
    when(categoryRepository.save(org.mockito.ArgumentMatchers.any(CategoryDocument.class)))
        .thenAnswer(invocation -> {
          CategoryDocument doc = invocation.getArgument(0);
          doc.setId("cat-1");
          return doc;
        });

    CategoryResponse response = categoryService.createCategory(request);

    assertThat(response.id()).isEqualTo("cat-1");
    assertThat(response.slug()).isEqualTo("eletronicos");
    assertThat(response.ancestors()).isEmpty();
    assertThat(response.effectiveAttributeDefinitions()).hasSize(1);
    assertThat(response.effectiveAttributeDefinitions().get(0).key()).isEqualTo("voltage");

    verify(cacheService).evict(CategoryService.CACHE_KEY_TREE);
  }

  @Test
  @DisplayName("Should inherit parent ancestors and attributes when creating subcategory")
  void shouldInheritParentAncestorsAndAttributes_whenCreatingSubcategory() {
    CategoryDocument parent = new CategoryDocument(
        "cat-root",
        "eletronicos",
        "Eletrônicos",
        null,
        List.of(),
        List.of(new AttributeDefinition("brand", "Brand", AttributeDefinition.AttributeType.STRING, null, true, true, List.of())),
        null
    );

    when(categoryRepository.findById("cat-root")).thenReturn(Optional.of(parent));
    when(categoryRepository.existsBySlug("smartphones")).thenReturn(false);
    when(categoryRepository.save(org.mockito.ArgumentMatchers.any(CategoryDocument.class)))
        .thenAnswer(invocation -> {
          CategoryDocument doc = invocation.getArgument(0);
          doc.setId("cat-sub");
          return doc;
        });

    CreateCategoryRequest request = new CreateCategoryRequest(
        "Smartphones",
        "smartphones",
        "cat-root",
        List.of(new AttributeDefinition("ram", "RAM", AttributeDefinition.AttributeType.NUMBER, "GB", false, true, List.of()))
    );

    CategoryResponse response = categoryService.createCategory(request);

    assertThat(response.id()).isEqualTo("cat-sub");
    assertThat(response.ancestors()).containsExactly("cat-root");
    assertThat(response.effectiveAttributeDefinitions()).extracting("key").containsExactly("brand", "ram");
  }

  @Test
  @DisplayName("Should throw DomainException with INVALID_ATTRIBUTE_DEFINITION when subcategory redefines inherited attribute key")
  void shouldThrowException_whenSubcategoryRedefinesInheritedKey() {
    CategoryDocument parent = new CategoryDocument(
        "cat-root",
        "eletronicos",
        "Eletrônicos",
        null,
        List.of(),
        List.of(new AttributeDefinition("brand", "Brand", AttributeDefinition.AttributeType.STRING, null, true, true, List.of())),
        null
    );

    when(categoryRepository.findById("cat-root")).thenReturn(Optional.of(parent));
    when(categoryRepository.existsBySlug("smartphones")).thenReturn(false);

    CreateCategoryRequest request = new CreateCategoryRequest(
        "Smartphones",
        "smartphones",
        "cat-root",
        List.of(new AttributeDefinition("brand", "Smartphone Brand", AttributeDefinition.AttributeType.STRING, null, true, true, List.of()))
    );

    assertThatThrownBy(() -> categoryService.createCategory(request))
        .isInstanceOf(DomainException.class)
        .matches(ex -> ((DomainException) ex).getFailureCode() == FailureCode.INVALID_ATTRIBUTE_DEFINITION);
  }

  @Test
  @DisplayName("Should throw DomainException with INVALID_ATTRIBUTE_DEFINITION when attribute key is blank")
  void shouldThrowException_whenAttributeKeyIsBlank() {
    CreateCategoryRequest request = new CreateCategoryRequest(
        "Eletrônicos",
        "eletronicos",
        null,
        List.of(new AttributeDefinition("", "Voltage", AttributeDefinition.AttributeType.STRING, "V", false, true, List.of()))
    );

    assertThatThrownBy(() -> categoryService.createCategory(request))
        .isInstanceOf(DomainException.class)
        .matches(ex -> ((DomainException) ex).getFailureCode() == FailureCode.INVALID_ATTRIBUTE_DEFINITION);
  }

  @Test
  @DisplayName("Should throw DomainException with INVALID_ATTRIBUTE_DEFINITION when attribute label is blank")
  void shouldThrowException_whenAttributeLabelIsBlank() {
    CreateCategoryRequest request = new CreateCategoryRequest(
        "Eletrônicos",
        "eletronicos",
        null,
        List.of(new AttributeDefinition("voltage", "  ", AttributeDefinition.AttributeType.STRING, "V", false, true, List.of()))
    );

    assertThatThrownBy(() -> categoryService.createCategory(request))
        .isInstanceOf(DomainException.class)
        .matches(ex -> ((DomainException) ex).getFailureCode() == FailureCode.INVALID_ATTRIBUTE_DEFINITION);
  }

  @Test
  @DisplayName("Should throw DomainException with INVALID_ATTRIBUTE_DEFINITION when attribute type is null")
  void shouldThrowException_whenAttributeTypeIsNull() {
    CreateCategoryRequest request = new CreateCategoryRequest(
        "Eletrônicos",
        "eletronicos",
        null,
        List.of(new AttributeDefinition("voltage", "Voltage", null, "V", false, true, List.of()))
    );

    assertThatThrownBy(() -> categoryService.createCategory(request))
        .isInstanceOf(DomainException.class)
        .matches(ex -> ((DomainException) ex).getFailureCode() == FailureCode.INVALID_ATTRIBUTE_DEFINITION);
  }

  @Test
  @DisplayName("Should throw DomainException with DUPLICATE_SLUG when category slug already exists")
  void shouldThrowException_whenSlugAlreadyExists() {
    when(categoryRepository.existsBySlug("eletronicos")).thenReturn(true);

    CreateCategoryRequest request = new CreateCategoryRequest("Eletrônicos", "eletronicos", null, List.of());

    assertThatThrownBy(() -> categoryService.createCategory(request))
        .isInstanceOf(DomainException.class)
        .matches(ex -> ((DomainException) ex).getFailureCode() == FailureCode.DUPLICATE_SLUG);
  }

  @Test
  @DisplayName("Should throw DomainException with CATEGORY_NOT_EMPTY when deleting category with subcategories")
  void shouldThrowException_whenDeletingCategoryWithSubcategories() {
    CategoryDocument doc = new CategoryDocument("cat-1", "eletronicos", "Eletrônicos", null, List.of(), List.of(), null);
    when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(doc));
    when(categoryRepository.existsByParentId("cat-1")).thenReturn(true);

    assertThatThrownBy(() -> categoryService.deleteCategory("cat-1"))
        .isInstanceOf(DomainException.class)
        .matches(ex -> ((DomainException) ex).getFailureCode() == FailureCode.CATEGORY_NOT_EMPTY);
  }

  @Test
  @DisplayName("Should delete category and evict cache when category has no subcategories and no products")
  void shouldDeleteCategory_whenNoSubcategoriesAndNoProducts() {
    CategoryDocument doc = new CategoryDocument("cat-1", "eletronicos", "Eletrônicos", null, List.of(), List.of(), null);
    when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(doc));
    when(categoryRepository.existsByParentId("cat-1")).thenReturn(false);

    categoryService.deleteCategory("cat-1");

    verify(categoryRepository).delete(doc);
    verify(cacheService).evict(CategoryService.CACHE_KEY_TREE);
  }

  @Test
  @DisplayName("Should return category path with ancestors and self when querying category path")
  void shouldReturnCategoryPath_whenQueryingCategoryPath() {
    CategoryDocument doc = new CategoryDocument("cat-sub", "smartphones", "Smartphones", "cat-root", List.of("cat-root"), List.of(), null);
    when(categoryRepository.findById("cat-sub")).thenReturn(Optional.of(doc));

    List<String> path = categoryService.getCategoryPath("cat-sub");

    assertThat(path).containsExactly("cat-root", "cat-sub");
  }

  @Test
  @DisplayName("Should check category existence when checking existsById")
  void shouldReturnTrue_whenCategoryExists() {
    when(categoryRepository.existsById("cat-1")).thenReturn(true);

    boolean exists = categoryService.existsById("cat-1");

    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("Should return category view when category exists by id")
  void shouldReturnCategoryView_whenCategoryExistsById() {
    CategoryDocument doc = new CategoryDocument("cat-1", "eletronicos", "Eletrônicos", null, List.of(), List.of(), null);
    when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(doc));

    var result = categoryService.findById("cat-1");

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo("cat-1");
    assertThat(result.get().slug()).isEqualTo("eletronicos");
  }

  @Test
  @DisplayName("Should return category view when category exists by slug")
  void shouldReturnCategoryView_whenCategoryExistsBySlug() {
    CategoryDocument doc = new CategoryDocument("cat-1", "eletronicos", "Eletrônicos", null, List.of(), List.of(), null);
    when(categoryRepository.findBySlug("eletronicos")).thenReturn(Optional.of(doc));

    var result = categoryService.findBySlug("eletronicos");

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo("cat-1");
    assertThat(result.get().slug()).isEqualTo("eletronicos");
  }
}
