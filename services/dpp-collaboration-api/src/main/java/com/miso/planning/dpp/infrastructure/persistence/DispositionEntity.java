package com.miso.planning.dpp.infrastructure.persistence;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="miso_disposition")
class DispositionEntity {
  @Id @Column(name="disposition_id") UUID dispositionId;
  @Column(name="review_id", nullable=false) UUID reviewId;
  @Column(name="correction_version", nullable=false) int correctionVersion;
  @Column(nullable=false) String decision;
  String comment;
  @Column(name="created_at", nullable=false) Instant createdAt;
  protected DispositionEntity() {}
}
