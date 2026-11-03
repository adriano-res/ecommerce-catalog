package com.ecommerce.shared.db.migrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

@ChangeUnit(id = "V002_CreateProductIndexes", order = "002", author = "ecommerce-catalog")
public class V002_CreateProductIndexes {

  private static final String COLLECTION_NAME = "products";

  @Execution
  public void execute(MongoTemplate mongoTemplate) {
    if (!mongoTemplate.collectionExists(COLLECTION_NAME)) {
      mongoTemplate.createCollection(COLLECTION_NAME);
    }

    mongoTemplate
        .indexOps(COLLECTION_NAME)
        .ensureIndex(new Index().on("sku", Sort.Direction.ASC).unique().named("uk_products_sku"));

    mongoTemplate
        .indexOps(COLLECTION_NAME)
        .ensureIndex(new Index().on("slug", Sort.Direction.ASC).unique().named("uk_products_slug"));

    mongoTemplate
        .indexOps(COLLECTION_NAME)
        .ensureIndex(
            new Index()
                .on("status", Sort.Direction.ASC)
                .on("categoryPath", Sort.Direction.ASC)
                .on("price.amountMinor", Sort.Direction.ASC)
                .named("idx_products_status_category_price"));

    mongoTemplate
        .indexOps(COLLECTION_NAME)
        .ensureIndex(
            new Index()
                .on("attributes.k", Sort.Direction.ASC)
                .on("attributes.v", Sort.Direction.ASC)
                .named("idx_products_attributes_kv"));

    TextIndexDefinition textIndex =
        new TextIndexDefinition.TextIndexDefinitionBuilder()
            .onField("name", 10F)
            .onField("brand", 5F)
            .onField("description", 1F)
            .withDefaultLanguage("portuguese")
            .named("idx_products_text")
            .build();

    mongoTemplate.indexOps(COLLECTION_NAME).ensureIndex(textIndex);
  }

  @RollbackExecution
  public void rollback(MongoTemplate mongoTemplate) {
    if (mongoTemplate.collectionExists(COLLECTION_NAME)) {
      mongoTemplate.indexOps(COLLECTION_NAME).dropIndex("uk_products_sku");
      mongoTemplate.indexOps(COLLECTION_NAME).dropIndex("uk_products_slug");
      mongoTemplate.indexOps(COLLECTION_NAME).dropIndex("idx_products_status_category_price");
      mongoTemplate.indexOps(COLLECTION_NAME).dropIndex("idx_products_attributes_kv");
      mongoTemplate.indexOps(COLLECTION_NAME).dropIndex("idx_products_text");
    }
  }
}
