package com.ecommerce.outbox;

import com.ecommerce.outbox.entity.OutboxDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service orchestrating atomic outbox event publishing within caller transactions.
 */
@Service
public class OutboxService implements OutboxApi {

  private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

  private final OutboxRepository outboxRepository;

  public OutboxService(OutboxRepository outboxRepository) {
    this.outboxRepository = outboxRepository;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRED)
  public void publish(String eventType, String payload) {
    OutboxDocument document = new OutboxDocument(eventType, payload);
    outboxRepository.save(document);
    log.debug("Published outbox event type={}, payload={}", eventType, payload);
  }
}
