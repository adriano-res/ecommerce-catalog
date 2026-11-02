package com.ecommerce.shared.db.migrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@ChangeUnit(id = "V001_CreateCategoryIndexes", order = "001", author = "ecommerce-catalog")
public class V001_CreateCategoryIndexes {

  private static final String COLLECTION_NAME = "categories";

  @Execution
  public void execute(MongoTemplate mongoTemplate) {
    if (!mongoTemplate.collectionExists(COLLECTION_NAME)) {
      mongoTemplate.createCollection(COLLECTION_NAME);
    }

    mongoTemplate.indexOps(COLLECTION_NAME).ensureIndex(
        new Index().on("slug", Sort.Direction.ASC).unique().named("uk_categories_slug")
    );

    mongoTemplate.indexOps(COLLECTION_NAME).ensureIndex(
        new Index().on("parentId", Sort.Direction.ASC).named("idx_categories_parent_id")
    );

    mongoTemplate.indexOps(COLLECTION_NAME).ensureIndex(
        new Index().on("ancestors", Sort.Direction.ASC).named("idx_categories_ancestors")
    );
  }

  @RollbackExecution
  public void rollback(MongoTemplate mongoTemplate) {
    if (mongoTemplate.collectionExists(COLLECTION_NAME)) {
      mongoTemplate.indexOps(COLLECTION_NAME).dropIndex("uk_categories_slug");
      mongoTemplate.indexOps(COLLECTION_NAME).dropIndex("idx_categories_parent_id");
      mongoTemplate.indexOps(COLLECTION_NAME).dropIndex("idx_categories_ancestors");
    }
  }
}
