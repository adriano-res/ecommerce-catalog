package com.ecommerce.product;

import com.ecommerce.product.entity.ProductDocument;
import com.ecommerce.product.entity.ProductStatus;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/** Data access repository for product document persistence in MongoDB. */
@Repository
public interface ProductRepository extends MongoRepository<ProductDocument, String> {

  Optional<ProductDocument> findBySku(String sku);

  Optional<ProductDocument> findBySlug(String slug);

  boolean existsBySku(String sku);

  boolean existsBySlug(String slug);

  boolean existsByCategoryId(String categoryId);

  long countByStatus(ProductStatus status);
}
