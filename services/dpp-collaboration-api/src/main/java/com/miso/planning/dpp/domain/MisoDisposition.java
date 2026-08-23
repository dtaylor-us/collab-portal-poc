package com.miso.planning.dpp.domain;
import java.time.Instant;
import java.util.UUID;
public record MisoDisposition(UUID dispositionId, UUID reviewId, int correctionVersion, Decision decision, String comment, Instant createdAt) {
  public enum Decision { ACCEPT_CORRECTION, REJECT_REWORK }
}
