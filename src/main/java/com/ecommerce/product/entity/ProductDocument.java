package com.ecommerce.product.entity;

import com.ecommerce.shared.audit.AuditMetadata;
import com.ecommerce.shared.money.Money;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/** MongoDB document entity representing a catalog product under the Attribute Pattern. */
@Document(collection = "products")
public class ProductDocument {

  @Id private String id;

  private String sku;
  private String slug;
  private String name;
  private String description;
  private String brand;
  private String categoryId;
  private List<String> categoryPath = new ArrayList<>();
  private ProductStatus status = ProductStatus.DRAFT;
  private Money price;
  private List<AttributeValue> attributes = new ArrayList<>();
  private List<ProductImage> images = new ArrayList<>();

  @Version private Long version;

  private AuditMetadata audit;

  public ProductDocument() {}

  public ProductDocument(
      String id,
      String sku,
      String slug,
      String name,
      String description,
      String brand,
      String categoryId,
      List<String> categoryPath,
      ProductStatus status,
      Money price,
      List<AttributeValue> attributes,
      List<ProductImage> images,
      Long version,
      AuditMetadata audit) {
    this.id = id;
    this.sku = sku;
    this.slug = slug;
    this.name = name;
    this.description = description;
    this.brand = brand;
    this.categoryId = categoryId;
    this.categoryPath = (categoryPath != null) ? new ArrayList<>(categoryPath) : new ArrayList<>();
    this.status = (status != null) ? status : ProductStatus.DRAFT;
    this.price = price;
    this.attributes = (attributes != null) ? new ArrayList<>(attributes) : new ArrayList<>();
    this.images = (images != null) ? new ArrayList<>(images) : new ArrayList<>();
    this.version = version;
    this.audit = audit;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }

  public List<String> getCategoryPath() {
    return categoryPath;
  }

  public void setCategoryPath(List<String> categoryPath) {
    this.categoryPath = (categoryPath != null) ? new ArrayList<>(categoryPath) : new ArrayList<>();
  }

  public ProductStatus getStatus() {
    return status;
  }

  public void setStatus(ProductStatus status) {
    this.status = status;
  }

  public Money getPrice() {
    return price;
  }

  public void setPrice(Money price) {
    this.price = price;
  }

  public List<AttributeValue> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<AttributeValue> attributes) {
    this.attributes = (attributes != null) ? new ArrayList<>(attributes) : new ArrayList<>();
  }

  public List<ProductImage> getImages() {
    return images;
  }

  public void setImages(List<ProductImage> images) {
    this.images = (images != null) ? new ArrayList<>(images) : new ArrayList<>();
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public AuditMetadata getAudit() {
    return audit;
  }

  public void setAudit(AuditMetadata audit) {
    this.audit = audit;
  }
}
