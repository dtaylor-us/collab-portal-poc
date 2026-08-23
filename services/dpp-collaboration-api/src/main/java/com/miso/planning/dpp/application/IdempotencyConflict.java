package com.miso.planning.dpp.application;
public final class IdempotencyConflict extends RuntimeException {
  public IdempotencyConflict(String message) { super(message); }
}
