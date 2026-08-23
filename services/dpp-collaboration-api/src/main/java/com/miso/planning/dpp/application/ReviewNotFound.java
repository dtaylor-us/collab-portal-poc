package com.miso.planning.dpp.application;
import java.util.UUID;
public final class ReviewNotFound extends RuntimeException {
  public ReviewNotFound(UUID id) { super("Review " + id + " was not found"); }
}
