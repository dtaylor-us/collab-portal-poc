package com.miso.planning.dpp.application;

import com.miso.planning.dpp.domain.Correction;
import com.miso.planning.dpp.domain.DppReview;
import com.miso.planning.dpp.domain.MisoDisposition;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DppReviewService {
  private static final Logger LOG = LoggerFactory.getLogger(DppReviewService.class);
  private final DppReviewRepository reviews;
  private final CollaborationHistoryRepository history;
  private final DppReviewWorkflow workflow;
  private final CommandLedger ledger;
  private final TransactionTemplate transactions;
  private final Clock clock;

  @Autowired
  public DppReviewService(DppReviewRepository reviews, CollaborationHistoryRepository history,
      DppReviewWorkflow workflow, CommandLedger ledger, TransactionTemplate transactions) {
    this(reviews, history, workflow, ledger, transactions, Clock.systemUTC());
  }
  DppReviewService(DppReviewRepository reviews, CollaborationHistoryRepository history,
      DppReviewWorkflow workflow, CommandLedger ledger, TransactionTemplate transactions, Clock clock) {
    this.reviews = reviews; this.history = history; this.workflow = workflow; this.ledger = ledger;
    this.transactions = transactions; this.clock = clock;
  }

  public DppReview create(String key, String resultId, String ownerId) {
    if (key == null || key.isBlank()) throw new IllegalArgumentException("Idempotency-Key header is required");
    UUID reviewId = UUID.nameUUIDFromBytes(("dpp-review:" + key).getBytes(StandardCharsets.UTF_8));
    String fingerprint = hash(resultId + "\u0000" + ownerId);
    DppReview review = transactions.execute(s -> {
      CommandLedger.CommandClaim claim = ledger.claim(key, "CREATE", reviewId, fingerprint);
      if (claim.completed() || !claim.firstAttempt()) return current(reviewId);
      return reviews.save(DppReview.publish(reviewId, resultId, ownerId, now()));
    });
    if (review.processInstanceKey() != null) return review;
    try {
      long processKey = workflow.startReview(review);
      return transactions.execute(s -> { DppReview correlated = reviews.save(review.correlate(processKey, now())); ledger.complete(key); return correlated; });
    } catch (RuntimeException failure) { throw inconsistent(reviewId, key, failure); }
  }

  public DppReview acceptByTo(UUID id, String key) {
    return command(id, key, "TO_ACCEPT", "", DppReviewWorkflow.WorkflowTask.TO_REVIEW,
        Map.of("toDecision", "ACCEPT"), r -> r.acceptByTo(now()), null, null);
  }
  public DppReview submitCorrection(UUID id, String key, String comment) {
    DppReview existing = current(id);
    DppReviewWorkflow.WorkflowTask task = existing.status().name().equals("REWORK_REQUIRED")
        || (existing.status().name().equals("PENDING_MISO_REVIEW") && history.corrections(id).size() > 1)
        ? DppReviewWorkflow.WorkflowTask.TO_REWORK : DppReviewWorkflow.WorkflowTask.TO_REVIEW;
    return command(id, key, "SUBMIT_CORRECTION", comment,
        task,
        task == DppReviewWorkflow.WorkflowTask.TO_REWORK ? Map.of() : Map.of("toDecision", "CORRECTION"),
        r -> r.submitCorrection(now()), comment, null);
  }
  public DppReview acceptCorrection(UUID id, String key, String comment) {
    return command(id, key, "MISO_ACCEPT", comment, DppReviewWorkflow.WorkflowTask.MISO_REVIEW,
        Map.of("misoDecision", "ACCEPT_CORRECTION"), r -> r.acceptCorrection(now()), null, MisoDisposition.Decision.ACCEPT_CORRECTION);
  }
  public DppReview rejectForRework(UUID id, String key, String comment) {
    return command(id, key, "MISO_REJECT", comment, DppReviewWorkflow.WorkflowTask.MISO_REVIEW,
        Map.of("misoDecision", "REJECT_REWORK"), r -> r.rejectForRework(now()), null, MisoDisposition.Decision.REJECT_REWORK);
  }

  private DppReview command(UUID id, String key, String operation, String payload,
      DppReviewWorkflow.WorkflowTask task, Map<String,Object> variables,
      java.util.function.UnaryOperator<DppReview> transition, String correction,
      MisoDisposition.Decision disposition) {
    String fingerprint = hash(payload == null ? "" : payload);
    CommandResult result = transactions.execute(s -> {
      CommandLedger.CommandClaim claim = ledger.claim(key, operation, id, fingerprint);
      if (claim.completed()) return new CommandResult(current(id), false);
      if (!claim.firstAttempt()) return new CommandResult(current(id), true);
      DppReview changed = reviews.save(transition.apply(current(id)));
      if (correction != null) history.addCorrection(id, correction);
      if (disposition != null) {
        List<Correction> corrections = history.corrections(id);
        if (corrections.isEmpty()) throw new IllegalStateException("A disposition requires a correction");
        history.addDisposition(id, corrections.get(corrections.size() - 1).version(), disposition, payload);
      }
      return new CommandResult(changed, true);
    });
    if (!result.advanceWorkflow()) return result.review();
    try {
      workflow.completeTask(result.review(), task, variables);
      transactions.executeWithoutResult(s -> ledger.complete(key));
      LOG.info("collaboration command completed reviewId={} processInstanceKey={} commandKey={} operation={}", id, result.review().processInstanceKey(), key, operation);
      return result.review();
    } catch (RuntimeException failure) { throw inconsistent(id, key, failure); }
  }
  private WorkflowConsistencyException inconsistent(UUID id, String key, RuntimeException cause) {
    LOG.error("database mutation committed but workflow command failed reviewId={} commandKey={}", id, key, cause);
    return new WorkflowConsistencyException("Business state was persisted for review " + id + "; retry with the same Idempotency-Key to reconcile workflow", cause);
  }
  public DppReview get(UUID id) { return current(id); }
  public List<DppReview> list() { return reviews.findAll(); }
  public List<Correction> corrections(UUID id) { current(id); return history.corrections(id); }
  public List<MisoDisposition> dispositions(UUID id) { current(id); return history.dispositions(id); }
  private DppReview current(UUID id) { return reviews.findById(id).orElseThrow(() -> new ReviewNotFound(id)); }
  private Instant now() { return clock.instant(); }
  private static String hash(String input) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
  private record CommandResult(DppReview review, boolean advanceWorkflow) {}
}
