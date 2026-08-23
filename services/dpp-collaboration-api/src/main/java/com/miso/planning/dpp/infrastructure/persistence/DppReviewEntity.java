package com.miso.planning.dpp.infrastructure.persistence;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="dpp_review")
class DppReviewEntity {
  @Id @Column(name="review_id") UUID reviewId;
  @Column(name="dpp_result_id", nullable=false) String dppResultId;
  @Column(name="transmission_owner_id", nullable=false) String transmissionOwnerId;
  @Column(nullable=false) String status;
  @Column(name="process_instance_key") Long processInstanceKey;
  @Column(name="created_at", nullable=false) Instant createdAt;
  @Column(name="updated_at", nullable=false) Instant updatedAt;
  protected DppReviewEntity() {}
}
