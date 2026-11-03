package com.ecommerce.outbox.jobs;

import com.ecommerce.outbox.OutboxRepository;
import com.ecommerce.outbox.entity.OutboxDocument;
import com.ecommerce.outbox.entity.OutboxStatus;
import com.ecommerce.shared.storage.StorageService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background worker processing pending transactional outbox events with retries.
 */
@Component
public class OutboxProcessor {

  private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);
  private static final int MAX_RETRIES = 5;
  private static final String EVENT_TYPE_DELETE_STORAGE_FILE = "DELETE_STORAGE_FILE";

  private final OutboxRepository outboxRepository;
  private final StorageService storageService;

  public OutboxProcessor(OutboxRepository outboxRepository, StorageService storageService) {
    this.outboxRepository = outboxRepository;
    this.storageService = storageService;
  }

  @Scheduled(fixedDelayString = "${app.outbox.interval-ms:5000}")
  public void processPendingEvents() {
    List<OutboxDocument> pendingEvents =
        outboxRepository.findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

    for (OutboxDocument event : pendingEvents) {
      processEvent(event);
    }

    List<OutboxDocument> retryableFailedEvents =
        outboxRepository.findTop20ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxStatus.FAILED, MAX_RETRIES);

    for (OutboxDocument event : retryableFailedEvents) {
      processEvent(event);
    }
  }

  public void processEvent(OutboxDocument event) {
    if (!EVENT_TYPE_DELETE_STORAGE_FILE.equals(event.getEventType())) {
      log.warn("Unsupported outbox event type '{}', marking as failed", event.getEventType());
      event.markFailed("Unsupported event type: " + event.getEventType());
      outboxRepository.save(event);
      return;
    }

    try {
      storageService.delete(event.getPayload());
      event.markCompleted();
      outboxRepository.save(event);
      log.info(
          "Successfully processed outbox event id={}, type={}, payload={}",
          event.getId(),
          event.getEventType(),
          event.getPayload());
    } catch (Exception ex) {
      if (event.getRetryCount() + 1 >= MAX_RETRIES) {
        event.markFailed("Exceeded max retries: " + ex.getMessage());
        log.error(
            "Outbox event id={} permanently failed after {} retries: {}",
            event.getId(),
            MAX_RETRIES,
            ex.getMessage());
      } else {
        event.incrementRetry(ex.getMessage());
        log.warn(
            "Failed processing outbox event id={}, retrying (attempt={}): {}",
            event.getId(),
            event.getRetryCount(),
            ex.getMessage());
      }
      outboxRepository.save(event);
    }
  }
}
