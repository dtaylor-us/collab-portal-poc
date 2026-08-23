package com.miso.planning.dpp.application;
import java.util.UUID;
public interface CommandLedger {
  CommandClaim claim(String key, String operation, UUID reviewId, String requestFingerprint);
  void complete(String key);
  record CommandClaim(boolean firstAttempt, boolean completed) {}
}
