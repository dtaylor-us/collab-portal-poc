package com.miso.planning.dpp.infrastructure.persistence;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="correction_submission")
class CorrectionEntity {
  @Id @Column(name="correction_id") UUID correctionId;
  @Column(name="review_id", nullable=false) UUID reviewId;
  @Column(nullable=false) int version;
  @Column(nullable=false) String comment;
  @Column(name="created_at", nullable=false) Instant createdAt;
  protected CorrectionEntity() {}
}
