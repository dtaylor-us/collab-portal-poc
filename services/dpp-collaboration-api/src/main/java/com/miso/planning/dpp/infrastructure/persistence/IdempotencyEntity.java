package com.miso.planning.dpp.infrastructure.persistence;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="idempotency_command")
class IdempotencyEntity {
  @Id @Column(name="idempotency_key") String key;
  @Column(nullable=false) String operation;
  @Column(name="review_id", nullable=false) UUID reviewId;
  @Column(name="request_hash", nullable=false) String requestHash;
  @Column(nullable=false) String state;
  @Column(name="created_at", nullable=false) Instant createdAt;
  @Column(name="completed_at") Instant completedAt;
  protected IdempotencyEntity() {}
}
