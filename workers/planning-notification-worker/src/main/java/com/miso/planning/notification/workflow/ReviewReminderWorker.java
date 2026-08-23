package com.miso.planning.notification.workflow;

import com.miso.planning.notification.application.NotificationDelivery;
import com.miso.planning.notification.application.ReminderNotification;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.annotation.JobWorker;
import java.util.Map;
import java.time.Duration;
import io.camunda.client.exception.CamundaError;
import org.springframework.stereotype.Component;

@Component
public class ReviewReminderWorker {

  private final NotificationDelivery notificationDelivery;

  public ReviewReminderWorker(NotificationDelivery notificationDelivery) {
    this.notificationDelivery = notificationDelivery;
  }

  @JobWorker(
      type = "send-to-review-reminder",
      fetchVariables = {"dppResultId", "transmissionOwnerId"},
      retryBackoff = 5000L)
  public void sendReviewReminder(ActivatedJob job) {
    Map<String, Object> variables = job.getVariablesAsMap();
    String notificationId = job.getProcessInstanceKey() + ":TO_REVIEW_REMINDER:1";
    try {
      notificationDelivery.sendReviewReminder(new ReminderNotification(notificationId, job.getProcessInstanceKey(),
          optionalString(variables.get("dppResultId")), optionalString(variables.get("transmissionOwnerId"))));
    } catch (RuntimeException failure) {
      throw CamundaError.jobError("Notification provider failed for " + notificationId, null,
          Math.max(0, job.getRetries() - 1), Duration.ofSeconds(5), failure);
    }
  }

  private String optionalString(Object value) {
    return value instanceof String stringValue && !stringValue.isBlank() ? stringValue : null;
  }
}
