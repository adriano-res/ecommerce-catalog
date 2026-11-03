package com.ecommerce.category;

import com.ecommerce.category.dto.AttributeDefinition;
import com.ecommerce.category.dto.CategoryResponse;
import com.ecommerce.category.dto.CategoryTreeResponse;
import com.ecommerce.category.dto.CategoryView;
import com.ecommerce.category.dto.CreateCategoryRequest;
import com.ecommerce.category.entity.CategoryDocument;
import com.ecommerce.category.mapper.CategoryMapper;
import com.ecommerce.shared.audit.AuditMetadata;
import com.ecommerce.shared.cache.CacheService;
import com.ecommerce.shared.error.DomainException;
import com.ecommerce.shared.error.FailureCode;
import com.ecommerce.shared.util.SlugUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

  private static final Logger log = LoggerFactory.getLogger(CategoryService.class);
  public static final String CACHE_KEY_TREE = "cache:category-tree";
  private static final Duration CACHE_TREE_TTL = Duration.ofMinutes(30);

  private final CategoryRepository categoryRepository;
  private final CacheService cacheService;
  private final AuditorAware<String> auditorAware;
  private final MongoTemplate mongoTemplate;

  public CategoryService(
      CategoryRepository categoryRepository,
      CacheService cacheService,
      AuditorAware<String> auditorAware,
      MongoTemplate mongoTemplate) {
    this.categoryRepository = categoryRepository;
    this.cacheService = cacheService;
    this.auditorAware = auditorAware;
    this.mongoTemplate = mongoTemplate;
  }

  @Transactional
  public CategoryResponse createCategory(CreateCategoryRequest request) {
    String slug = SlugUtils.resolveSlug(request.name(), request.slug());
    if (categoryRepository.existsBySlug(slug)) {
      throw new DomainException(FailureCode.DUPLICATE_SLUG, "Category with slug '" + slug + "' already exists");
    }

    List<String> ancestors = new ArrayList<>();
    List<AttributeDefinition> inheritedAttributes = new ArrayList<>();

    if (request.parentId() != null && !request.parentId().isBlank()) {
      CategoryDocument parent = categoryRepository.findById(request.parentId())
          .orElseThrow(() -> new DomainException(FailureCode.CATEGORY_NOT_FOUND, "Parent category not found: " + request.parentId()));

      if (parent.getAncestors() != null) {
        ancestors.addAll(parent.getAncestors());
      }
      ancestors.add(parent.getId());

      inheritedAttributes = resolveEffectiveAttributes(parent);
    }

    List<AttributeDefinition> localDefinitions = (request.attributeDefinitions() != null)
        ? request.attributeDefinitions()
        : Collections.emptyList();

    validateAttributeDefinitions(localDefinitions, inheritedAttributes);

    String actor = auditorAware.getCurrentAuditor().orElse("system");
    AuditMetadata audit = AuditMetadata.create(actor);

    CategoryDocument doc = new CategoryDocument(
        null,
        slug,
        request.name().trim(),
        request.parentId(),
        ancestors,
        localDefinitions,
        audit
    );

    CategoryDocument saved = categoryRepository.save(doc);
    log.info("Created category id={}, slug={}, parentId={}", saved.getId(), saved.getSlug(), saved.getParentId());

    cacheService.evict(CACHE_KEY_TREE);

    List<AttributeDefinition> effectiveAttributes = mergeAttributes(inheritedAttributes, localDefinitions);
    return CategoryMapper.toResponse(saved, effectiveAttributes);
  }

  public CategoryResponse getByIdOrSlug(String idOrSlug) {
    CategoryDocument doc = categoryRepository.findById(idOrSlug)
        .or(() -> categoryRepository.findBySlug(idOrSlug))
        .orElseThrow(() -> new DomainException(FailureCode.CATEGORY_NOT_FOUND, "Category not found: " + idOrSlug));

    List<AttributeDefinition> effective = resolveEffectiveAttributes(doc);
    return CategoryMapper.toResponse(doc, effective);
  }

  public List<CategoryTreeResponse> getCategoryTree() {
    return cacheService.get(CACHE_KEY_TREE, new TypeReference<List<CategoryTreeResponse>>() {})
        .orElseGet(() -> {
          List<CategoryDocument> allDocs = categoryRepository.findAll();
          List<CategoryTreeResponse> tree = CategoryMapper.buildTree(allDocs);
          cacheService.put(CACHE_KEY_TREE, tree, CACHE_TREE_TTL);
          return tree;
        });
  }

  @Transactional
  public void deleteCategory(String id) {
    CategoryDocument doc = categoryRepository.findById(id)
        .orElseThrow(() -> new DomainException(FailureCode.CATEGORY_NOT_FOUND, "Category not found: " + id));

    if (categoryRepository.existsByParentId(id)) {
      throw new DomainException(FailureCode.CATEGORY_NOT_EMPTY, "Cannot delete category with subcategories: " + id);
    }

    if (hasAssociatedProducts(id)) {
      throw new DomainException(FailureCode.CATEGORY_NOT_EMPTY, "Cannot delete category with associated products: " + id);
    }

    categoryRepository.delete(doc);
    log.info("Deleted category id={}, slug={}", doc.getId(), doc.getSlug());

    cacheService.evict(CACHE_KEY_TREE);
  }

  public Optional<CategoryView> findById(String id) {
    return categoryRepository.findById(id)
        .map(doc -> CategoryMapper.toView(doc, resolveEffectiveAttributes(doc)));
  }

  public Optional<CategoryView> findBySlug(String slug) {
    return categoryRepository.findBySlug(slug)
        .map(doc -> CategoryMapper.toView(doc, resolveEffectiveAttributes(doc)));
  }

  public boolean existsById(String id) {
    return categoryRepository.existsById(id);
  }

  public List<AttributeDefinition> getEffectiveAttributes(String categoryId) {
    return categoryRepository.findById(categoryId)
        .map(this::resolveEffectiveAttributes)
        .orElse(Collections.emptyList());
  }

  public List<String> getCategoryPath(String categoryId) {
    return categoryRepository.findById(categoryId)
        .map(doc -> {
          List<String> path = new ArrayList<>();
          if (doc.getAncestors() != null) {
            path.addAll(doc.getAncestors());
          }
          path.add(doc.getId());
          return path;
        })
        .orElse(Collections.emptyList());
  }

  public List<AttributeDefinition> resolveEffectiveAttributes(CategoryDocument category) {
    List<AttributeDefinition> result = new ArrayList<>();
    if (category.getAncestors() != null && !category.getAncestors().isEmpty()) {
      List<CategoryDocument> ancestorDocs = categoryRepository.findByIdIn(category.getAncestors());
      Map<String, CategoryDocument> map = new LinkedHashMap<>();
      for (CategoryDocument anc : ancestorDocs) {
        map.put(anc.getId(), anc);
      }
      for (String ancestorId : category.getAncestors()) {
        CategoryDocument anc = map.get(ancestorId);
        if (anc != null && anc.getAttributeDefinitions() != null) {
          result = mergeAttributes(result, anc.getAttributeDefinitions());
        }
      }
    }

    if (category.getAttributeDefinitions() != null) {
      result = mergeAttributes(result, category.getAttributeDefinitions());
    }
    return result;
  }

  private void validateAttributeDefinitions(
      List<AttributeDefinition> localDefinitions,
      List<AttributeDefinition> inheritedAttributes) {
    Set<String> inheritedKeys = new HashSet<>();
    for (AttributeDefinition attr : inheritedAttributes) {
      if (attr.key() != null) {
        inheritedKeys.add(attr.key().toLowerCase(Locale.ROOT));
      }
    }

    Set<String> seenLocalKeys = new HashSet<>();
    for (AttributeDefinition local : localDefinitions) {
      if (local.key() == null || local.key().isBlank()) {
        throw new DomainException(FailureCode.INVALID_ATTRIBUTE_DEFINITION, "Attribute key must not be blank");
      }
      if (local.label() == null || local.label().isBlank()) {
        throw new DomainException(FailureCode.INVALID_ATTRIBUTE_DEFINITION, "Attribute label must not be blank");
      }
      if (local.type() == null) {
        throw new DomainException(FailureCode.INVALID_ATTRIBUTE_DEFINITION, "Attribute type must not be null");
      }
      String lowerKey = local.key().trim().toLowerCase(Locale.ROOT);
      if (inheritedKeys.contains(lowerKey)) {
        throw new DomainException(
            FailureCode.INVALID_ATTRIBUTE_DEFINITION,
            "Attribute key '" + local.key() + "' overrides an inherited attribute definition from an ancestor");
      }
      if (!seenLocalKeys.add(lowerKey)) {
        throw new DomainException(
            FailureCode.INVALID_ATTRIBUTE_DEFINITION,
            "Duplicate attribute key in request: " + local.key());
      }
      if (local.type() == AttributeDefinition.AttributeType.ENUM) {
        if (local.allowedValues() == null || local.allowedValues().isEmpty()) {
          throw new DomainException(
              FailureCode.INVALID_ATTRIBUTE_DEFINITION,
              "ENUM attribute '" + local.key() + "' must define at least one allowed value");
        }
      }
    }
  }

  private List<AttributeDefinition> mergeAttributes(
      List<AttributeDefinition> base,
      List<AttributeDefinition> addition) {
    Map<String, AttributeDefinition> map = new LinkedHashMap<>();
    if (base != null) {
      for (AttributeDefinition a : base) {
        map.put(a.key(), a);
      }
    }
    if (addition != null) {
      for (AttributeDefinition a : addition) {
        map.put(a.key(), a);
      }
    }
    return new ArrayList<>(map.values());
  }

  private boolean hasAssociatedProducts(String categoryId) {
    if (mongoTemplate == null || !mongoTemplate.collectionExists("products")) {
      return false;
    }
    Query query = new Query(Criteria.where("categoryId").is(categoryId));
    return mongoTemplate.exists(query, "products");
  }
}
