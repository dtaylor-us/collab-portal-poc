package com.miso.planning.dpp.infrastructure.persistence;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
interface ReviewJpaStore extends JpaRepository<DppReviewEntity, UUID> {}
interface CorrectionJpaStore extends JpaRepository<CorrectionEntity, UUID> {
  List<CorrectionEntity> findByReviewIdOrderByVersion(UUID reviewId);
  long countByReviewId(UUID reviewId);
}
interface DispositionJpaStore extends JpaRepository<DispositionEntity, UUID> {
  List<DispositionEntity> findByReviewIdOrderByCreatedAt(UUID reviewId);
}
interface IdempotencyJpaStore extends JpaRepository<IdempotencyEntity, String> {}
