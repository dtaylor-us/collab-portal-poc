package com.miso.planning.dpp.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DppReview(
    UUID reviewId,
    String dppResultId,
    String transmissionOwnerId,
    ReviewStatus status,
    Long processInstanceKey,
    Instant createdAt,
    Instant updatedAt) {

  public DppReview {
    Objects.requireNonNull(reviewId);
    requireText(dppResultId, "dppResultId");
    requireText(transmissionOwnerId, "transmissionOwnerId");
    Objects.requireNonNull(status);
    Objects.requireNonNull(createdAt);
    Objects.requireNonNull(updatedAt);
  }

  public static DppReview publish(UUID id, String resultId, String ownerId, Instant now) {
    return new DppReview(id, resultId, ownerId, ReviewStatus.PENDING_TO_REVIEW, null, now, now);
  }

  public DppReview correlate(long key, Instant now) {
    return new DppReview(reviewId, dppResultId, transmissionOwnerId, status, key, createdAt, now);
  }

  public DppReview acceptByTo(Instant now) { return transition(ReviewStatus.PENDING_TO_REVIEW, ReviewStatus.COMPLETED, now); }
  public DppReview submitCorrection(Instant now) {
    if (status != ReviewStatus.PENDING_TO_REVIEW && status != ReviewStatus.REWORK_REQUIRED) invalid("submit a correction");
    return withStatus(ReviewStatus.PENDING_MISO_REVIEW, now);
  }
  public DppReview acceptCorrection(Instant now) { return transition(ReviewStatus.PENDING_MISO_REVIEW, ReviewStatus.COMPLETED, now); }
  public DppReview rejectForRework(Instant now) { return transition(ReviewStatus.PENDING_MISO_REVIEW, ReviewStatus.REWORK_REQUIRED, now); }

  private DppReview transition(ReviewStatus expected, ReviewStatus next, Instant now) {
    if (status != expected) invalid("apply this decision");
    return withStatus(next, now);
  }
  private DppReview withStatus(ReviewStatus next, Instant now) {
    return new DppReview(reviewId, dppResultId, transmissionOwnerId, next, processInstanceKey, createdAt, now);
  }
  private void invalid(String action) { throw new InvalidReviewTransition("Cannot " + action + " while review is " + status); }
  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
  }
}
