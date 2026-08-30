package com.miso.planning.dpp.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.miso.planning.dpp.domain.Correction;
import com.miso.planning.dpp.domain.DppReview;
import com.miso.planning.dpp.domain.MisoDisposition;
import com.miso.planning.dpp.domain.ReviewStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class DppReviewServiceConsistencyTest {

  private InMemoryReviews reviews;
  private InMemoryHistory history;
  private InMemoryLedger ledger;
  private RecordingWorkflow workflow;
  private DppReviewService service;

  @BeforeEach
  void setUp() {
    reviews = new InMemoryReviews();
    history = new InMemoryHistory();
    ledger = new InMemoryLedger();
    workflow = new RecordingWorkflow();
    Clock clock = Clock.fixed(Instant.parse("2026-08-23T12:00:00Z"), ZoneOffset.UTC);
    service = new DppReviewService(reviews, history, workflow, ledger,
        new TransactionTemplate(new TestTransactionManager()), clock);
  }

  @Test
  void retryingCreateAfterWorkflowFailureCorrelatesExistingReviewWithoutDuplicateMutation() {
    workflow.failNextStart = true;

    assertThrows(WorkflowConsistencyException.class,
        () -> service.create("create-1", "DPP-1", "TO-1"));

    assertEquals(1, reviews.values.size());
    DppReview persisted = reviews.values.values().iterator().next();
    assertEquals(ReviewStatus.PENDING_TO_REVIEW, persisted.status());
    assertEquals(null, persisted.processInstanceKey());
    assertEquals(1, workflow.startAttempts);

    DppReview recovered = service.create("create-1", "DPP-1", "TO-1");

    assertEquals(1, reviews.values.size());
    assertEquals(2, workflow.startAttempts);
    assertEquals(1001L, recovered.processInstanceKey());
    assertEquals(ReviewStatus.PENDING_TO_REVIEW, recovered.status());
    assertEquals(true, ledger.entries.get("create-1").completed);
  }

  @Test
  void retryingCorrectionAfterWorkflowFailureDoesNotCreateSecondCorrection() {
    DppReview review = service.create("create-2", "DPP-2", "TO-2");
    workflow.failNextCompletion = true;

    assertThrows(WorkflowConsistencyException.class,
        () -> service.submitCorrection(review.reviewId(), "correction-1", "Please correct constraint"));

    assertEquals(ReviewStatus.PENDING_MISO_REVIEW, service.get(review.reviewId()).status());
    assertEquals(1, service.corrections(review.reviewId()).size());
    assertEquals(1, workflow.completionAttempts);

    DppReview recovered = service.submitCorrection(
        review.reviewId(), "correction-1", "Please correct constraint");

    assertEquals(ReviewStatus.PENDING_MISO_REVIEW, recovered.status());
    assertEquals(1, service.corrections(review.reviewId()).size());
    assertEquals(2, workflow.completionAttempts);
    assertEquals(true, ledger.entries.get("correction-1").completed);
  }

  @Test
  void replayingCompletedCorrectionDoesNotAdvanceWorkflowAgain() {
    DppReview review = service.create("create-3", "DPP-3", "TO-3");

    service.submitCorrection(review.reviewId(), "correction-2", "Correction");
    int completionsAfterFirstRequest = workflow.completionAttempts;

    service.submitCorrection(review.reviewId(), "correction-2", "Correction");

    assertEquals(1, service.corrections(review.reviewId()).size());
    assertEquals(completionsAfterFirstRequest, workflow.completionAttempts);
  }

  @Test
  void reusingIdempotencyKeyWithDifferentPayloadIsRejected() {
    DppReview review = service.create("create-4", "DPP-4", "TO-4");
    service.submitCorrection(review.reviewId(), "correction-3", "Original correction");

    assertThrows(IllegalStateException.class,
        () -> service.submitCorrection(review.reviewId(), "correction-3", "Different correction"));

    assertEquals(1, service.corrections(review.reviewId()).size());
  }

  private static final class InMemoryReviews implements DppReviewRepository {
    private final Map<UUID, DppReview> values = new HashMap<>();
    @Override public DppReview save(DppReview review) { values.put(review.reviewId(), review); return review; }
    @Override public Optional<DppReview> findById(UUID id) { return Optional.ofNullable(values.get(id)); }
    @Override public List<DppReview> findAll() { return List.copyOf(values.values()); }
  }

  private static final class InMemoryHistory implements CollaborationHistoryRepository {
    private final Map<UUID, List<Correction>> corrections = new HashMap<>();
    private final Map<UUID, List<MisoDisposition>> dispositions = new HashMap<>();
    @Override public Correction addCorrection(UUID reviewId, String comment) {
      List<Correction> values = corrections.computeIfAbsent(reviewId, ignored -> new ArrayList<>());
      Correction correction = new Correction(UUID.randomUUID(), reviewId, values.size() + 1, comment, Instant.now());
      values.add(correction);
      return correction;
    }
    @Override public List<Correction> corrections(UUID reviewId) {
      return List.copyOf(corrections.getOrDefault(reviewId, List.of()));
    }
    @Override public MisoDisposition addDisposition(UUID reviewId, int correctionVersion,
        MisoDisposition.Decision decision, String comment) {
      MisoDisposition disposition = new MisoDisposition(
          UUID.randomUUID(), reviewId, correctionVersion, decision, comment, Instant.now());
      dispositions.computeIfAbsent(reviewId, ignored -> new ArrayList<>()).add(disposition);
      return disposition;
    }
    @Override public List<MisoDisposition> dispositions(UUID reviewId) {
      return List.copyOf(dispositions.getOrDefault(reviewId, List.of()));
    }
  }

  private static final class InMemoryLedger implements CommandLedger {
    private final Map<String, Entry> entries = new HashMap<>();
    @Override public CommandClaim claim(String key, String operation, UUID reviewId, String fingerprint) {
      Entry existing = entries.get(key);
      if (existing == null) {
        entries.put(key, new Entry(operation, reviewId, fingerprint));
        return new CommandClaim(true, false);
      }
      if (!existing.operation.equals(operation) || !existing.reviewId.equals(reviewId)
          || !existing.fingerprint.equals(fingerprint)) {
        throw new IllegalStateException("Idempotency key was already used for a different request");
      }
      return new CommandClaim(false, existing.completed);
    }
    @Override public void complete(String key) { entries.get(key).completed = true; }
    private static final class Entry {
      final String operation; final UUID reviewId; final String fingerprint; boolean completed;
      Entry(String operation, UUID reviewId, String fingerprint) {
        this.operation = operation; this.reviewId = reviewId; this.fingerprint = fingerprint;
      }
    }
  }

  private static final class RecordingWorkflow implements DppReviewWorkflow {
    private boolean failNextStart;
    private boolean failNextCompletion;
    private int startAttempts;
    private int completionAttempts;
    @Override public long startReview(DppReview review) {
      startAttempts++;
      if (failNextStart) { failNextStart = false; throw new IllegalStateException("simulated workflow outage"); }
      return 1001L;
    }
    @Override public void completeTask(DppReview review, WorkflowTask task, Map<String, Object> routingVariables) {
      completionAttempts++;
      if (failNextCompletion) { failNextCompletion = false; throw new IllegalStateException("simulated workflow outage"); }
    }
  }

  private static final class TestTransactionManager extends AbstractPlatformTransactionManager {
    @Override protected Object doGetTransaction() { return new Object(); }
    @Override protected void doBegin(Object transaction, TransactionDefinition definition) {}
    @Override protected void doCommit(DefaultTransactionStatus status) throws TransactionException {}
    @Override protected void doRollback(DefaultTransactionStatus status) throws TransactionException {}
  }
}
