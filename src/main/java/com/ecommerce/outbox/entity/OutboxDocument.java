package com.ecommerce.outbox.entity;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Persisted outbox event representing an asynchronous task scheduled atomically with domain state.
 */
@Document("outbox_events")
public class OutboxDocument {

  @Id
  private String id;
  private String eventType;
  private String payload;
  private OutboxStatus status;
  private int retryCount;
  private String lastError;
  private Instant createdAt;
  private Instant updatedAt;

  public OutboxDocument() {}

  public OutboxDocument(String eventType, String payload) {
    this.eventType = eventType;
    this.payload = payload;
    this.status = OutboxStatus.PENDING;
    this.retryCount = 0;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public void markCompleted() {
    this.status = OutboxStatus.COMPLETED;
    this.updatedAt = Instant.now();
  }

  public void markFailed(String error) {
    this.status = OutboxStatus.FAILED;
    this.lastError = error;
    this.updatedAt = Instant.now();
  }

  public void incrementRetry(String error) {
    this.retryCount++;
    this.lastError = error;
    this.updatedAt = Instant.now();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public void setStatus(OutboxStatus status) {
    this.status = status;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(int retryCount) {
    this.retryCount = retryCount;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
