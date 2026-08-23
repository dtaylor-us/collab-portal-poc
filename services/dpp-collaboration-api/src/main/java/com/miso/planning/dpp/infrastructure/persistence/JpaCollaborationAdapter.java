package com.miso.planning.dpp.infrastructure.persistence;

import com.miso.planning.dpp.application.*;
import com.miso.planning.dpp.domain.*;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCollaborationAdapter implements DppReviewRepository, CollaborationHistoryRepository, CommandLedger {
  private final ReviewJpaStore reviews; private final CorrectionJpaStore corrections;
  private final DispositionJpaStore dispositions; private final IdempotencyJpaStore commands;
  private final Clock clock = Clock.systemUTC();
  public JpaCollaborationAdapter(ReviewJpaStore reviews, CorrectionJpaStore corrections, DispositionJpaStore dispositions, IdempotencyJpaStore commands) {
    this.reviews=reviews; this.corrections=corrections; this.dispositions=dispositions; this.commands=commands;
  }
  public DppReview save(DppReview d) { DppReviewEntity e=new DppReviewEntity(); e.reviewId=d.reviewId(); e.dppResultId=d.dppResultId(); e.transmissionOwnerId=d.transmissionOwnerId(); e.status=d.status().name(); e.processInstanceKey=d.processInstanceKey(); e.createdAt=d.createdAt(); e.updatedAt=d.updatedAt(); return map(reviews.save(e)); }
  public java.util.Optional<DppReview> findById(UUID id) { return reviews.findById(id).map(this::map); }
  public List<DppReview> findAll() { return reviews.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"createdAt")).stream().map(this::map).toList(); }
  public Correction addCorrection(UUID id,String comment) { CorrectionEntity e=new CorrectionEntity(); e.correctionId=UUID.randomUUID();e.reviewId=id;e.version=Math.toIntExact(corrections.countByReviewId(id)+1);e.comment=comment;e.createdAt=clock.instant();return map(corrections.save(e)); }
  public List<Correction> corrections(UUID id) { return corrections.findByReviewIdOrderByVersion(id).stream().map(this::map).toList(); }
  public MisoDisposition addDisposition(UUID id,int version,MisoDisposition.Decision decision,String comment) { DispositionEntity e=new DispositionEntity();e.dispositionId=UUID.randomUUID();e.reviewId=id;e.correctionVersion=version;e.decision=decision.name();e.comment=comment;e.createdAt=clock.instant();return map(dispositions.save(e)); }
  public List<MisoDisposition> dispositions(UUID id) { return dispositions.findByReviewIdOrderByCreatedAt(id).stream().map(this::map).toList(); }
  public CommandClaim claim(String key,String operation,UUID reviewId,String fingerprint) {
    if (key==null || key.isBlank()) throw new IllegalArgumentException("Idempotency-Key header is required");
    var found=commands.findById(key);
    if(found.isPresent()){var e=found.get();if(!e.operation.equals(operation)||!e.reviewId.equals(reviewId)||!e.requestHash.equals(fingerprint))throw new IdempotencyConflict("Idempotency-Key was already used for a different command or payload");return new CommandClaim(false,"COMPLETED".equals(e.state));}
    IdempotencyEntity e=new IdempotencyEntity();e.key=key;e.operation=operation;e.reviewId=reviewId;e.requestHash=fingerprint;e.state="BUSINESS_APPLIED";e.createdAt=clock.instant();commands.saveAndFlush(e);return new CommandClaim(true,false);
  }
  public void complete(String key){IdempotencyEntity e=commands.findById(key).orElseThrow();e.state="COMPLETED";e.completedAt=clock.instant();commands.save(e);}
  private DppReview map(DppReviewEntity e){return new DppReview(e.reviewId,e.dppResultId,e.transmissionOwnerId,ReviewStatus.valueOf(e.status),e.processInstanceKey,e.createdAt,e.updatedAt);}
  private Correction map(CorrectionEntity e){return new Correction(e.correctionId,e.reviewId,e.version,e.comment,e.createdAt);}
  private MisoDisposition map(DispositionEntity e){return new MisoDisposition(e.dispositionId,e.reviewId,e.correctionVersion,MisoDisposition.Decision.valueOf(e.decision),e.comment,e.createdAt);}
}
