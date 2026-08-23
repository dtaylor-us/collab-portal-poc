package com.miso.planning.dpp.infrastructure.workflow;

import com.miso.planning.dpp.application.DppReviewWorkflow;
import com.miso.planning.dpp.domain.DppReview;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.enums.UserTaskState;
import io.camunda.client.api.search.response.UserTask;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CamundaDppReviewWorkflowAdapter implements DppReviewWorkflow {
  private static final String PROCESS_ID = "dpp-result-review";
  private final CamundaClient client;
  private final boolean simulateFailure;
  public CamundaDppReviewWorkflowAdapter(CamundaClient client, @Value("${workflow.simulate-failure:false}") boolean simulateFailure) {
    this.client=client; this.simulateFailure=simulateFailure;
  }
  @Override public long startReview(DppReview review) {
    failIfRequested();
    return client.newCreateInstanceCommand().bpmnProcessId(PROCESS_ID).latestVersion()
        .variables(Map.of("reviewId", review.reviewId().toString(), "dppResultId", review.dppResultId(), "transmissionOwnerId", review.transmissionOwnerId()))
        .send().join().getProcessInstanceKey();
  }
  @Override public void completeTask(DppReview review, WorkflowTask task, Map<String,Object> variables) {
    failIfRequested();
    if (review.processInstanceKey()==null) throw new WorkflowTaskException("Review is not correlated to a process instance");
    String elementId=switch(task){case TO_REVIEW->"Activity_18oc3tj";case MISO_REVIEW->"Activity_16lvyrc";case TO_REWORK->"Activity_147ueuz";};
    var response=client.newUserTaskSearchRequest().filter(f -> f.processInstanceKey(review.processInstanceKey()).elementId(elementId).state(UserTaskState.CREATED)).send().join();
    if(response.items().size()!=1) throw new WorkflowTaskException("Expected one active " + task + " task for process " + review.processInstanceKey() + " but found " + response.items().size());
    UserTask userTask=response.singleItem();
    client.newCompleteUserTaskCommand(userTask.getUserTaskKey()).variables(variables).send().join();
  }
  private void failIfRequested(){if(simulateFailure)throw new WorkflowTaskException("Simulated workflow adapter failure");}
}
