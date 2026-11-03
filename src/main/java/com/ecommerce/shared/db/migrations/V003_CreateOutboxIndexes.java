package com.ecommerce.shared.db.migrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@ChangeUnit(id = "V003_CreateOutboxIndexes", order = "003", author = "ecommerce-catalog")
public class V003_CreateOutboxIndexes {

  private static final String COLLECTION_NAME = "outbox_events";

  @Execution
  public void execute(MongoTemplate mongoTemplate) {
    if (!mongoTemplate.collectionExists(COLLECTION_NAME)) {
      mongoTemplate.createCollection(COLLECTION_NAME);
    }

    mongoTemplate
        .indexOps(COLLECTION_NAME)
        .ensureIndex(
            new Index()
                .on("status", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.ASC)
                .named("idx_outbox_events_status_created_at"));
  }

  @RollbackExecution
  public void rollback(MongoTemplate mongoTemplate) {
    if (mongoTemplate.collectionExists(COLLECTION_NAME)) {
      mongoTemplate.indexOps(COLLECTION_NAME).dropIndex("idx_outbox_events_status_created_at");
    }
  }
}
