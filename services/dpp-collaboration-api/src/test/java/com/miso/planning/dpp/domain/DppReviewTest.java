package com.miso.planning.dpp.domain;
import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class DppReviewTest {
  private final Instant now=Instant.parse("2026-01-01T00:00:00Z");
  @Test void supportsCorrectionReworkAndAcceptance(){
    DppReview review=DppReview.publish(UUID.randomUUID(),"DPP-1","TO-1",now);
    review=review.submitCorrection(now).rejectForRework(now).submitCorrection(now).acceptCorrection(now);
    assertThat(review.status()).isEqualTo(ReviewStatus.COMPLETED);
  }
  @Test void supportsDirectAcceptance(){assertThat(DppReview.publish(UUID.randomUUID(),"DPP-1","TO-1",now).acceptByTo(now).status()).isEqualTo(ReviewStatus.COMPLETED);}
  @Test void rejectsInvalidTransition(){DppReview review=DppReview.publish(UUID.randomUUID(),"DPP-1","TO-1",now);assertThatThrownBy(() -> review.acceptCorrection(now)).isInstanceOf(InvalidReviewTransition.class);}
}
