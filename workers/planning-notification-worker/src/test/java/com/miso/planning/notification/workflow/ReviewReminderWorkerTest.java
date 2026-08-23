package com.miso.planning.notification.workflow;

import com.miso.planning.notification.application.NotificationDelivery;
import com.miso.planning.notification.application.ReminderNotification;
import io.camunda.client.api.response.ActivatedJob;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ReviewReminderWorkerTest {

  @Test
  void toleratesMissingOptionalCorrelationVariables() {
    AtomicReference<ReminderNotification> captured = new AtomicReference<>();
    NotificationDelivery delivery = captured::set;
    ActivatedJob job = (ActivatedJob) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ActivatedJob.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "getProcessInstanceKey" -> 42L;
          case "getVariablesAsMap" -> Map.of();
          default -> method.getReturnType().isPrimitive() ? 0 : null;
        });

    new ReviewReminderWorker(delivery).sendReviewReminder(job);

    assertThat(captured.get()).isEqualTo(new ReminderNotification("42:TO_REVIEW_REMINDER:1", 42L, null, null));
  }
}
