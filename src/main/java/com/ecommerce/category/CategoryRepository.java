package com.ecommerce.category;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends MongoRepository<CategoryDocument, String> {

  Optional<CategoryDocument> findBySlug(String slug);

  boolean existsBySlug(String slug);

  boolean existsByParentId(String parentId);

  List<CategoryDocument> findByParentId(String parentId);

  List<CategoryDocument> findByIdIn(List<String> ids);
}
