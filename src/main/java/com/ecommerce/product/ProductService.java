package com.ecommerce.product;

import com.ecommerce.category.CategoryService;
import com.ecommerce.category.dto.AttributeDefinition;
import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductSnapshot;
import com.ecommerce.product.dto.UpdateProductRequest;
import com.ecommerce.product.entity.AttributeValue;
import com.ecommerce.product.entity.ProductDocument;
import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.validation.AttributeValidator;
import com.ecommerce.product.validation.ImageValidator;
import com.ecommerce.outbox.OutboxApi;
import com.ecommerce.shared.audit.AuditMetadata;
import com.ecommerce.shared.error.DomainException;
import com.ecommerce.shared.error.FailureCode;
import com.ecommerce.shared.storage.StorageService;
import com.ecommerce.shared.util.SlugUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Business service orchestrating product operations, attribute validation, and
 * lifecycle.
 */
@Service
public class ProductService implements ProductApi {

  private static final Logger log = LoggerFactory.getLogger(ProductService.class);

  private final ProductRepository productRepository;
  private final CategoryService categoryService;
  private final AttributeValidator attributeValidator;
  private final AuditorAware<String> auditorAware;
  private final StorageService storageService;
  private final ImageValidator imageValidator;
  private final OutboxApi outboxApi;

  public ProductService(
      ProductRepository productRepository,
      CategoryService categoryService,
      AttributeValidator attributeValidator,
      AuditorAware<String> auditorAware,
      StorageService storageService,
      ImageValidator imageValidator,
      OutboxApi outboxApi) {
    this.productRepository = productRepository;
    this.categoryService = categoryService;
    this.attributeValidator = attributeValidator;
    this.auditorAware = auditorAware;
    this.storageService = storageService;
    this.imageValidator = imageValidator;
    this.outboxApi = outboxApi;
  }

  @Transactional
  public ProductResponse createProduct(CreateProductRequest request) {
    if (!categoryService.existsById(request.categoryId())) {
      throw new DomainException(
          FailureCode.CATEGORY_NOT_FOUND, "Category not found: " + request.categoryId());
    }

    String cleanSku = request.sku().trim();
    if (productRepository.existsBySku(cleanSku)) {
      throw new DomainException(
          FailureCode.DUPLICATE_SKU, "Product with SKU '" + cleanSku + "' already exists");
    }

    String slug = SlugUtils.resolveSlug(request.name(), request.slug());
    if (productRepository.existsBySlug(slug)) {
      throw new DomainException(
          FailureCode.DUPLICATE_SLUG, "Product with slug '" + slug + "' already exists");
    }

    List<AttributeDefinition> definitions = categoryService.getEffectiveAttributes(request.categoryId());
    attributeValidator.validate(request.attributes(), definitions);

    List<String> categoryPath = categoryService.getCategoryPath(request.categoryId());
    List<AttributeValue> attributeValues = ProductMapper.toAttributeValues(request.attributes(), definitions);

    String actor = auditorAware.getCurrentAuditor().orElse("system");
    AuditMetadata audit = AuditMetadata.create(actor);

    ProductDocument doc = new ProductDocument(
        null,
        cleanSku,
        slug,
        request.name().trim(),
        request.description(),
        request.brand(),
        request.categoryId(),
        categoryPath,
        ProductStatus.DRAFT,
        request.price(),
        attributeValues,
        request.images(),
        null,
        audit);

    ProductDocument saved = productRepository.save(doc);
    log.info(
        "Created product id={}, sku={}, slug={}, status={}",
        saved.getId(),
        saved.getSku(),
        saved.getSlug(),
        saved.getStatus());

    return ProductMapper.toResponse(saved);
  }

  @Transactional
  public ProductResponse updateProduct(
      String id, Long expectedVersion, UpdateProductRequest request) {
    ProductDocument doc = productRepository
        .findById(id)
        .orElseThrow(
            () -> new DomainException(
                FailureCode.PRODUCT_NOT_FOUND, "Product not found: " + id));

    if (expectedVersion != null
        && doc.getVersion() != null
        && !doc.getVersion().equals(expectedVersion)) {
      throw new DomainException(
          FailureCode.CONCURRENT_MODIFICATION,
          "Resource version mismatch: expected "
              + expectedVersion
              + " but found "
              + doc.getVersion());
    }

    if (request.categoryId() != null && !request.categoryId().isBlank()) {
      if (!categoryService.existsById(request.categoryId())) {
        throw new DomainException(
            FailureCode.CATEGORY_NOT_FOUND, "Category not found: " + request.categoryId());
      }
      doc.setCategoryId(request.categoryId());
      doc.setCategoryPath(categoryService.getCategoryPath(request.categoryId()));
    }

    if (request.name() != null && !request.name().isBlank()) {
      doc.setName(request.name().trim());
    }

    if (request.description() != null) {
      doc.setDescription(request.description());
    }

    if (request.brand() != null) {
      doc.setBrand(request.brand());
    }

    if (request.price() != null) {
      doc.setPrice(request.price());
    }

    if (request.images() != null) {
      doc.setImages(request.images());
    }

    if (request.attributes() != null) {
      List<AttributeDefinition> definitions = categoryService.getEffectiveAttributes(doc.getCategoryId());
      attributeValidator.validate(request.attributes(), definitions);
      doc.setAttributes(ProductMapper.toAttributeValues(request.attributes(), definitions));
    }

    String actor = auditorAware.getCurrentAuditor().orElse("system");
    if (doc.getAudit() != null) {
      doc.setAudit(doc.getAudit().withUpdate(actor));
    } else {
      doc.setAudit(AuditMetadata.create(actor));
    }

    ProductDocument saved = productRepository.save(doc);
    log.info(
        "Updated product id={}, sku={}, version={}",
        saved.getId(),
        saved.getSku(),
        saved.getVersion());

    return ProductMapper.toResponse(saved);
  }

  @Transactional
  public ProductResponse publishProduct(String id) {
    ProductDocument doc = productRepository
        .findById(id)
        .orElseThrow(
            () -> new DomainException(
                FailureCode.PRODUCT_NOT_FOUND, "Product not found: " + id));

    doc.setStatus(ProductStatus.ACTIVE);
    String actor = auditorAware.getCurrentAuditor().orElse("system");
    if (doc.getAudit() != null) {
      doc.setAudit(doc.getAudit().withUpdate(actor));
    } else {
      doc.setAudit(AuditMetadata.create(actor));
    }

    ProductDocument saved = productRepository.save(doc);
    log.info("Published product id={}, sku={}", saved.getId(), saved.getSku());

    return ProductMapper.toResponse(saved);
  }

  @Transactional
  public ProductResponse archiveProduct(String id) {
    ProductDocument doc = productRepository
        .findById(id)
        .orElseThrow(
            () -> new DomainException(
                FailureCode.PRODUCT_NOT_FOUND, "Product not found: " + id));

    doc.setStatus(ProductStatus.ARCHIVED);
    String actor = auditorAware.getCurrentAuditor().orElse("system");
    if (doc.getAudit() != null) {
      doc.setAudit(doc.getAudit().withUpdate(actor));
    } else {
      doc.setAudit(AuditMetadata.create(actor));
    }

    ProductDocument saved = productRepository.save(doc);
    log.info("Archived product id={}, sku={}", saved.getId(), saved.getSku());

    return ProductMapper.toResponse(saved);
  }

  public ProductResponse getByIdOrSlug(String idOrSlug, boolean publicOnly) {
    ProductDocument doc = productRepository
        .findById(idOrSlug)
        .or(() -> productRepository.findBySlug(idOrSlug))
        .orElseThrow(
            () -> new DomainException(
                FailureCode.PRODUCT_NOT_FOUND, "Product not found: " + idOrSlug));

    if (publicOnly && doc.getStatus() != ProductStatus.ACTIVE) {
      throw new DomainException(
          FailureCode.PRODUCT_NOT_FOUND, "Product not found: " + idOrSlug);
    }

    return ProductMapper.toResponse(doc);
  }

  public ProductResponse addImage(String productId, MultipartFile file, String alt) {
    ProductDocument product =
        productRepository
            .findById(productId)
            .orElseThrow(
                () ->
                    new DomainException(
                        FailureCode.PRODUCT_NOT_FOUND, "Product not found: " + productId));

    String imageId = UUID.randomUUID().toString();
    String extension = imageValidator.validateAndExtractExtension(file);
    String imageKey = "products/" + productId + "/" + imageId + "." + extension;

    String publicUrl;
    try {
      publicUrl =
          storageService.upload(
              imageKey, file.getInputStream(), file.getSize(), file.getContentType());
    } catch (IOException e) {
      throw new DomainException(
          FailureCode.INTERNAL_ERROR, "Failed to read image content for upload: " + e.getMessage());
    }

    String altText = (alt != null && !alt.isBlank()) ? alt.trim() : product.getName();
    ProductImage newImage = new ProductImage(imageId, publicUrl, altText);

    List<ProductImage> currentImages =
        product.getImages() != null ? new ArrayList<>(product.getImages()) : new ArrayList<>();
    currentImages.add(newImage);
    product.setImages(currentImages);

    String actor = auditorAware.getCurrentAuditor().orElse("system");
    if (product.getAudit() != null) {
      product.setAudit(product.getAudit().withUpdate(actor));
    } else {
      product.setAudit(AuditMetadata.create(actor));
    }

    try {
      ProductDocument saved = productRepository.save(product);
      log.info("Added image to product id={}, url={}", productId, publicUrl);
      return ProductMapper.toResponse(saved);
    } catch (Exception ex) {
      try {
        storageService.delete(imageKey);
      } catch (Exception s3Ex) {
        log.warn(
            "Direct storage compensation failed for key={}, publishing outbox deletion event",
            imageKey,
            s3Ex);
        outboxApi.publish("DELETE_STORAGE_FILE", imageKey);
      }
      throw ex;
    }
  }

  @Transactional
  public ProductResponse removeImage(String productId, String imageIdOrTarget) {
    ProductDocument product =
        productRepository
            .findById(productId)
            .orElseThrow(
                () ->
                    new DomainException(
                        FailureCode.PRODUCT_NOT_FOUND, "Product not found: " + productId));

    if (imageIdOrTarget == null || imageIdOrTarget.isBlank()) {
      throw new DomainException(
          FailureCode.RESOURCE_NOT_FOUND, "Image identifier must be provided");
    }

    String cleanTarget = imageIdOrTarget.trim();
    List<ProductImage> currentImages =
        product.getImages() != null ? new ArrayList<>(product.getImages()) : new ArrayList<>();

    ProductImage matchedImage =
        currentImages.stream()
            .filter(
                img ->
                    (img.id() != null && img.id().equals(cleanTarget))
                        || (img.url() != null
                            && (img.url().equals(cleanTarget) || img.url().contains(cleanTarget))))
            .findFirst()
            .orElseThrow(
                () ->
                    new DomainException(
                        FailureCode.RESOURCE_NOT_FOUND,
                        "Image not found for product: " + imageIdOrTarget));

    currentImages.remove(matchedImage);
    product.setImages(currentImages);

    String resolvedKey = resolveStorageKey(matchedImage.url(), cleanTarget);

    String actor = auditorAware.getCurrentAuditor().orElse("system");
    if (product.getAudit() != null) {
      product.setAudit(product.getAudit().withUpdate(actor));
    } else {
      product.setAudit(AuditMetadata.create(actor));
    }

    ProductDocument saved = productRepository.save(product);
    outboxApi.publish("DELETE_STORAGE_FILE", resolvedKey);

    log.info(
        "Removed image from product id={}, key={} published for outbox deletion",
        productId,
        resolvedKey);
    return ProductMapper.toResponse(saved);
  }

  private String resolveStorageKey(String url, String target) {
    if (target.startsWith("products/")) {
      return target;
    }
    if (url != null && url.contains("products/")) {
      return url.substring(url.indexOf("products/"));
    }
    return target;
  }

  @Override
  public Optional<ProductSnapshot> findById(String id) {
    return productRepository.findById(id).map(ProductMapper::toSnapshot);
  }

  @Override
  public Optional<ProductSnapshot> findBySlug(String slug) {
    return productRepository.findBySlug(slug).map(ProductMapper::toSnapshot);
  }

  @Override
  public Optional<ProductSnapshot> findBySku(String sku) {
    return productRepository.findBySku(sku).map(ProductMapper::toSnapshot);
  }

  @Override
  public boolean existsByCategoryId(String categoryId) {
    return productRepository.existsByCategoryId(categoryId);
  }

  @Override
  public long countByStatus(ProductStatus status) {
    return productRepository.countByStatus(status);
  }
}
