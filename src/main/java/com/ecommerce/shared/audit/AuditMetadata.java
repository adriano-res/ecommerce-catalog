package com.ecommerce.shared.audit;

import java.time.Instant;
import java.util.Objects;

/**
 * Embedded audit metadata capturing creation and update timestamps and actors.
 */
public record AuditMetadata(
    Instant createdAt,
    String createdBy,
    Instant updatedAt,
    String updatedBy) {

  public AuditMetadata {
    Objects.requireNonNull(createdAt, "createdAt cannot be null");
    Objects.requireNonNull(createdBy, "createdBy cannot be null");
    Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    Objects.requireNonNull(updatedBy, "updatedBy cannot be null");
  }

  public static AuditMetadata create(String actor) {
    Instant now = Instant.now();
    String safeActor = (actor == null || actor.isBlank()) ? "system" : actor;
    return new AuditMetadata(now, safeActor, now, safeActor);
  }

  public AuditMetadata withUpdate(String actor) {
    String safeActor = (actor == null || actor.isBlank()) ? "system" : actor;
    return new AuditMetadata(this.createdAt, this.createdBy, Instant.now(), safeActor);
  }
}
