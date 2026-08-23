package com.miso.planning.dpp.web;

import com.miso.planning.dpp.application.DppReviewService;
import com.miso.planning.dpp.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dpp-reviews")
public class DppReviewController {
  private final DppReviewService service;
  public DppReviewController(DppReviewService service){this.service=service;}

  @PostMapping public ResponseEntity<ReviewResponse> create(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreateReviewRequest request){
    DppReview review=service.create(key,request.dppResultId(),request.transmissionOwnerId());
    return ResponseEntity.created(URI.create("/api/dpp-reviews/"+review.reviewId())).body(ReviewResponse.from(review));
  }
  @GetMapping public List<ReviewResponse> list(){return service.list().stream().map(ReviewResponse::from).toList();}
  @GetMapping("/{id}") public ReviewDetails get(@PathVariable UUID id){return details(service.get(id));}
  @GetMapping("/{id}/corrections") public List<Correction> corrections(@PathVariable UUID id){return service.corrections(id);}
  @PostMapping("/{id}/accept") public ReviewDetails accept(@PathVariable UUID id,@RequestHeader("Idempotency-Key") String key){return details(service.acceptByTo(id,key));}
  @PostMapping("/{id}/corrections") public ReviewDetails correction(@PathVariable UUID id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CommentRequest body){return details(service.submitCorrection(id,key,body.comment()));}
  @PostMapping("/{id}/miso-accept") public ReviewDetails misoAccept(@PathVariable UUID id,@RequestHeader("Idempotency-Key") String key,@RequestBody(required=false) CommentRequest body){return details(service.acceptCorrection(id,key,body==null?null:body.comment()));}
  @PostMapping("/{id}/miso-reject") public ReviewDetails misoReject(@PathVariable UUID id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CommentRequest body){return details(service.rejectForRework(id,key,body.comment()));}
  private ReviewDetails details(DppReview r){return new ReviewDetails(ReviewResponse.from(r),service.corrections(r.reviewId()),service.dispositions(r.reviewId()));}
  public record CreateReviewRequest(@NotBlank String dppResultId,@NotBlank String transmissionOwnerId){}
  public record CommentRequest(@NotBlank String comment){}
  public record ReviewResponse(UUID reviewId,String dppResultId,String transmissionOwnerId,ReviewStatus status,Long processInstanceKey,java.time.Instant createdAt,java.time.Instant updatedAt){static ReviewResponse from(DppReview r){return new ReviewResponse(r.reviewId(),r.dppResultId(),r.transmissionOwnerId(),r.status(),r.processInstanceKey(),r.createdAt(),r.updatedAt());}}
  public record ReviewDetails(ReviewResponse review,List<Correction> corrections,List<MisoDisposition> dispositions){}
}
