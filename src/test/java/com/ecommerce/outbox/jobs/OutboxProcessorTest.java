package com.ecommerce.outbox.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.outbox.OutboxRepository;
import com.ecommerce.outbox.entity.OutboxDocument;
import com.ecommerce.outbox.entity.OutboxStatus;
import com.ecommerce.shared.storage.StorageService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutboxProcessorTest {

  private OutboxRepository outboxRepository;
  private StorageService storageService;
  private OutboxProcessor outboxProcessor;

  @BeforeEach
  void setUp() {
    outboxRepository = mock(OutboxRepository.class);
    storageService = mock(StorageService.class);
    outboxProcessor = new OutboxProcessor(outboxRepository, storageService);
  }

  @Test
  @DisplayName("Should successfully delete file from storage and mark outbox event completed")
  void shouldDeleteFileAndMarkCompleted_whenValidDeleteStorageFileEventProcessed() {
    OutboxDocument event = new OutboxDocument("DELETE_STORAGE_FILE", "products/123/image.webp");
    event.setId("evt-1");

    outboxProcessor.processEvent(event);

    verify(storageService).delete("products/123/image.webp");
    verify(outboxRepository).save(event);
    assertThat(event.getStatus()).isEqualTo(OutboxStatus.COMPLETED);
    assertThat(event.getRetryCount()).isZero();
  }

  @Test
  @DisplayName("Should increment retry count and retain pending status on transient storage failure")
  void shouldIncrementRetry_whenStorageServiceThrowsTransientException() {
    OutboxDocument event = new OutboxDocument("DELETE_STORAGE_FILE", "products/123/image.webp");
    event.setId("evt-2");
    doThrow(new RuntimeException("Connection timeout")).when(storageService).delete("products/123/image.webp");

    outboxProcessor.processEvent(event);

    verify(outboxRepository).save(event);
    assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(event.getRetryCount()).isEqualTo(1);
    assertThat(event.getLastError()).contains("Connection timeout");
  }

  @Test
  @DisplayName("Should permanently mark event as failed when max retries exceeded")
  void shouldMarkFailed_whenMaxRetriesExceeded() {
    OutboxDocument event = new OutboxDocument("DELETE_STORAGE_FILE", "products/123/image.webp");
    event.setId("evt-3");
    event.setRetryCount(4);
    doThrow(new RuntimeException("S3 500 error")).when(storageService).delete("products/123/image.webp");

    outboxProcessor.processEvent(event);

    verify(outboxRepository).save(event);
    assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
    assertThat(event.getLastError()).contains("Exceeded max retries");
  }

  @Test
  @DisplayName("Should mark unsupported event type as failed immediately")
  void shouldMarkFailed_whenUnsupportedEventTypeEncountered() {
    OutboxDocument event = new OutboxDocument("UNKNOWN_ACTION", "payload-data");
    event.setId("evt-4");

    outboxProcessor.processEvent(event);

    verify(outboxRepository).save(event);
    assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
    assertThat(event.getLastError()).contains("Unsupported event type: UNKNOWN_ACTION");
  }

  @Test
  @DisplayName("Should process both pending and retryable failed events in scheduled cycle")
  void shouldProcessBothPendingAndRetryableEvents_whenScheduledExecutionRuns() {
    OutboxDocument pending = new OutboxDocument("DELETE_STORAGE_FILE", "products/p1/a.png");
    pending.setId("p-1");
    OutboxDocument failedRetryable = new OutboxDocument("DELETE_STORAGE_FILE", "products/p2/b.png");
    failedRetryable.setId("f-1");
    failedRetryable.setStatus(OutboxStatus.FAILED);
    failedRetryable.setRetryCount(2);

    when(outboxRepository.findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
        .thenReturn(List.of(pending));
    when(outboxRepository.findTop20ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(OutboxStatus.FAILED, 5))
        .thenReturn(List.of(failedRetryable));

    outboxProcessor.processPendingEvents();

    verify(storageService).delete("products/p1/a.png");
    verify(storageService).delete("products/p2/b.png");
    verify(outboxRepository).save(pending);
    verify(outboxRepository).save(failedRetryable);
    assertThat(pending.getStatus()).isEqualTo(OutboxStatus.COMPLETED);
    assertThat(failedRetryable.getStatus()).isEqualTo(OutboxStatus.COMPLETED);
  }
}
