package com.ecommerce.category;

import com.ecommerce.category.dto.AttributeDefinition;
import com.ecommerce.shared.audit.AuditMetadata;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "categories")
public class CategoryDocument {

  @Id
  private String id;

  private String slug;
  private String name;
  private String parentId;
  private List<String> ancestors = new ArrayList<>();
  private List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
  private AuditMetadata audit;

  @Version
  private Long version;

  public CategoryDocument() {}

  public CategoryDocument(
      String id,
      String slug,
      String name,
      String parentId,
      List<String> ancestors,
      List<AttributeDefinition> attributeDefinitions,
      AuditMetadata audit) {
    this.id = id;
    this.slug = slug;
    this.name = name;
    this.parentId = parentId;
    this.ancestors = (ancestors != null) ? new ArrayList<>(ancestors) : new ArrayList<>();
    this.attributeDefinitions = (attributeDefinitions != null) ? new ArrayList<>(attributeDefinitions) : new ArrayList<>();
    this.audit = audit;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public List<String> getAncestors() {
    return ancestors;
  }

  public void setAncestors(List<String> ancestors) {
    this.ancestors = (ancestors != null) ? new ArrayList<>(ancestors) : new ArrayList<>();
  }

  public List<AttributeDefinition> getAttributeDefinitions() {
    return attributeDefinitions;
  }

  public void setAttributeDefinitions(List<AttributeDefinition> attributeDefinitions) {
    this.attributeDefinitions = (attributeDefinitions != null) ? new ArrayList<>(attributeDefinitions) : new ArrayList<>();
  }

  public AuditMetadata getAudit() {
    return audit;
  }

  public void setAudit(AuditMetadata audit) {
    this.audit = audit;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
