package com.miso.planning.notification.workflow;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miso.planning.notification.application.NotificationDelivery;
import com.miso.planning.notification.application.ReminderNotification;
import io.camunda.client.api.response.ActivatedJob;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReviewReminderWorkerTest {

  @Test
  void toleratesMissingOptionalCorrelationVariables() {
    NotificationDelivery delivery = mock(NotificationDelivery.class);
    ActivatedJob job = mock(ActivatedJob.class);
    when(job.getProcessInstanceKey()).thenReturn(42L);
    when(job.getVariablesAsMap()).thenReturn(Map.of());

    new ReviewReminderWorker(delivery).sendReviewReminder(job);

    verify(delivery).sendReviewReminder(new ReminderNotification(42L, null, null));
  }
}
