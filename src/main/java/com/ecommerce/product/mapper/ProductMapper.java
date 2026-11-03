package com.ecommerce.product.mapper;

import com.ecommerce.category.dto.AttributeDefinition;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductSnapshot;
import com.ecommerce.product.entity.AttributeValue;
import com.ecommerce.product.entity.ProductDocument;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure conversion mapping between product domain documents, DTOs, and snapshots. */
public final class ProductMapper {

  private ProductMapper() {}

  public static List<AttributeValue> toAttributeValues(
      Map<String, Object> attributes, List<AttributeDefinition> definitions) {
    if (attributes == null || attributes.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String, AttributeDefinition> defMap = new LinkedHashMap<>();
    if (definitions != null) {
      for (AttributeDefinition def : definitions) {
        if (def.key() != null) {
          defMap.put(def.key(), def);
        }
      }
    }

    List<AttributeValue> list = new ArrayList<>();
    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      String key = entry.getKey();
      Object val = entry.getValue();
      AttributeDefinition def = defMap.get(key);
      String unit = (def != null) ? def.unit() : null;

      Object normalizedValue = val;
      if (def != null
          && def.type() == AttributeDefinition.AttributeType.NUMBER
          && val instanceof String s) {
        try {
          normalizedValue = Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
          try {
            normalizedValue = Double.parseDouble(s.trim());
          } catch (NumberFormatException ignored) {
          }
        }
      }

      list.add(new AttributeValue(key, normalizedValue, unit));
    }
    return list;
  }

  public static Map<String, Object> toAttributeMap(List<AttributeValue> attributeValues) {
    if (attributeValues == null || attributeValues.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, Object> map = new LinkedHashMap<>();
    for (AttributeValue av : attributeValues) {
      if (av != null && av.key() != null) {
        map.put(av.key(), av.value());
      }
    }
    return map;
  }

  public static ProductResponse toResponse(ProductDocument doc) {
    if (doc == null) {
      return null;
    }
    return new ProductResponse(
        doc.getId(),
        doc.getSku(),
        doc.getSlug(),
        doc.getName(),
        doc.getBrand(),
        doc.getDescription(),
        doc.getCategoryId(),
        (doc.getCategoryPath() != null) ? List.copyOf(doc.getCategoryPath()) : List.of(),
        doc.getStatus(),
        doc.getPrice(),
        toAttributeMap(doc.getAttributes()),
        (doc.getImages() != null) ? List.copyOf(doc.getImages()) : List.of(),
        (doc.getVersion() != null) ? doc.getVersion() : 0L,
        (doc.getAudit() != null) ? doc.getAudit().createdAt() : null,
        (doc.getAudit() != null) ? doc.getAudit().updatedAt() : null);
  }

  public static ProductSnapshot toSnapshot(ProductDocument doc) {
    if (doc == null) {
      return null;
    }
    return new ProductSnapshot(
        doc.getId(),
        doc.getSku(),
        doc.getSlug(),
        doc.getName(),
        doc.getBrand(),
        doc.getDescription(),
        doc.getCategoryId(),
        (doc.getCategoryPath() != null) ? List.copyOf(doc.getCategoryPath()) : List.of(),
        doc.getStatus(),
        doc.getPrice(),
        toAttributeMap(doc.getAttributes()),
        (doc.getImages() != null) ? List.copyOf(doc.getImages()) : List.of(),
        (doc.getVersion() != null) ? doc.getVersion() : 0L,
        doc.getAudit());
  }
}
