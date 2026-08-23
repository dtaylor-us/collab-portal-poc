package com.miso.planning.dpp.application;
import com.miso.planning.dpp.domain.DppReview;
import java.util.Map;
public interface DppReviewWorkflow {
  long startReview(DppReview review);
  void completeTask(DppReview review, WorkflowTask task, Map<String, Object> routingVariables);
  enum WorkflowTask { TO_REVIEW, MISO_REVIEW, TO_REWORK }
}
