package com.miso.planning.dpp.application;
import com.miso.planning.dpp.domain.DppReview;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface DppReviewRepository {
  DppReview save(DppReview review);
  Optional<DppReview> findById(UUID id);
  List<DppReview> findAll();
}
