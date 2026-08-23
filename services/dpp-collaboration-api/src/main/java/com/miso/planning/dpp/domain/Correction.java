package com.miso.planning.dpp.domain;
import java.time.Instant;
import java.util.UUID;
public record Correction(UUID correctionId, UUID reviewId, int version, String comment, Instant createdAt) {
  public Correction { if (comment == null || comment.isBlank()) throw new IllegalArgumentException("comment is required"); }
}
