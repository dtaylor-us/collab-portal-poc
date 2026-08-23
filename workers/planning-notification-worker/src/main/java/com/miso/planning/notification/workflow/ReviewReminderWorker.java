package com.miso.planning.notification.workflow;

import com.miso.planning.notification.application.NotificationDelivery;
import com.miso.planning.notification.application.ReminderNotification;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.annotation.JobWorker;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReviewReminderWorker {

  private final NotificationDelivery notificationDelivery;

  public ReviewReminderWorker(NotificationDelivery notificationDelivery) {
    this.notificationDelivery = notificationDelivery;
  }

  @JobWorker(
      type = "send-to-review-reminder",
      fetchVariables = {"dppResultId", "transmissionOwnerId"})
  public void sendReviewReminder(ActivatedJob job) {
    Map<String, Object> variables = job.getVariablesAsMap();
    notificationDelivery.sendReviewReminder(
        new ReminderNotification(
            job.getProcessInstanceKey(),
            optionalString(variables.get("dppResultId")),
            optionalString(variables.get("transmissionOwnerId"))));
  }

  private String optionalString(Object value) {
    return value instanceof String stringValue && !stringValue.isBlank() ? stringValue : null;
  }
}
