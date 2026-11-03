package com.ecommerce.outbox;

/**
 * Public facade exposing transactional outbox publishing capabilities to other modules.
 */
public interface OutboxApi {

  /**
   * Publishes an event task atomically within the caller's active database transaction.
   *
   * @param eventType descriptor identifier of the event task
   * @param payload string payload containing task arguments
   */
  void publish(String eventType, String payload);
}
