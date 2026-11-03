package com.ecommerce.outbox.entity;

/**
 * Execution status lifecycle for transactional outbox events.
 */
public enum OutboxStatus {
  PENDING,
  PROCESSING,
  COMPLETED,
  FAILED
}
