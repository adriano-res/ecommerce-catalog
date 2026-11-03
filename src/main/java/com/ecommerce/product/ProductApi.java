package com.ecommerce.product;

import com.ecommerce.product.dto.ProductSnapshot;
import com.ecommerce.product.entity.ProductStatus;
import java.util.Optional;

/** Public facade for cross-module interactions with the product domain. */
public interface ProductApi {

  Optional<ProductSnapshot> findById(String id);

  Optional<ProductSnapshot> findBySlug(String slug);

  Optional<ProductSnapshot> findBySku(String sku);

  boolean existsByCategoryId(String categoryId);

  long countByStatus(ProductStatus status);
}
