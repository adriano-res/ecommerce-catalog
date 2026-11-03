package com.ecommerce.outbox;

import com.ecommerce.outbox.entity.OutboxDocument;
import com.ecommerce.outbox.entity.OutboxStatus;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for transactional outbox persistence.
 */
public interface OutboxRepository extends MongoRepository<OutboxDocument, String> {

  List<OutboxDocument> findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus status);

  List<OutboxDocument> findTop20ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
      OutboxStatus status, int maxRetries);

  long countByStatus(OutboxStatus status);
}
