package com.ecommerce.category;

import com.ecommerce.category.dto.AttributeDefinition;
import com.ecommerce.category.dto.CategoryResponse;
import com.ecommerce.category.dto.CategoryTreeResponse;
import com.ecommerce.category.dto.CategoryView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CategoryMapper {

  private CategoryMapper() {}

  public static CategoryResponse toResponse(CategoryDocument doc, List<AttributeDefinition> effectiveAttributes) {
    if (doc == null) {
      return null;
    }
    return new CategoryResponse(
        doc.getId(),
        doc.getName(),
        doc.getSlug(),
        doc.getParentId(),
        doc.getAncestors(),
        doc.getAttributeDefinitions(),
        effectiveAttributes != null ? effectiveAttributes : doc.getAttributeDefinitions(),
        doc.getAudit()
    );
  }

  public static CategoryView toView(CategoryDocument doc, List<AttributeDefinition> effectiveAttributes) {
    if (doc == null) {
      return null;
    }
    return new CategoryView(
        doc.getId(),
        doc.getName(),
        doc.getSlug(),
        doc.getParentId(),
        doc.getAncestors(),
        doc.getAttributeDefinitions(),
        effectiveAttributes != null ? effectiveAttributes : doc.getAttributeDefinitions()
    );
  }

  public static List<CategoryTreeResponse> buildTree(List<CategoryDocument> allDocs) {
    if (allDocs == null || allDocs.isEmpty()) {
      return Collections.emptyList();
    }

    Map<String, List<CategoryDocument>> childrenByParentId = new LinkedHashMap<>();
    List<CategoryDocument> roots = new ArrayList<>();

    for (CategoryDocument doc : allDocs) {
      if (doc.getParentId() == null || doc.getParentId().isBlank()) {
        roots.add(doc);
      } else {
        childrenByParentId
            .computeIfAbsent(doc.getParentId(), k -> new ArrayList<>())
            .add(doc);
      }
    }

    List<CategoryTreeResponse> result = new ArrayList<>();
    for (CategoryDocument root : roots) {
      result.add(buildTreeNode(root, childrenByParentId));
    }
    return result;
  }

  private static CategoryTreeResponse buildTreeNode(
      CategoryDocument current,
      Map<String, List<CategoryDocument>> childrenByParentId) {
    List<CategoryDocument> childDocs = childrenByParentId.getOrDefault(current.getId(), Collections.emptyList());
    List<CategoryTreeResponse> childResponses = new ArrayList<>();
    for (CategoryDocument child : childDocs) {
      childResponses.add(buildTreeNode(child, childrenByParentId));
    }

    return new CategoryTreeResponse(
        current.getId(),
        current.getName(),
        current.getSlug(),
        current.getParentId(),
        current.getAttributeDefinitions(),
        childResponses
    );
  }
}
