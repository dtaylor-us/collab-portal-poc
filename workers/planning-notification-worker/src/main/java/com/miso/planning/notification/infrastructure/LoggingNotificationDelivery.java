package com.miso.planning.notification.infrastructure;

import com.miso.planning.notification.application.NotificationDelivery;
import com.miso.planning.notification.application.ReminderNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoggingNotificationDelivery implements NotificationDelivery {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingNotificationDelivery.class);

  private final boolean simulateFailure;
  private final Set<String> delivered = ConcurrentHashMap.newKeySet();

  public LoggingNotificationDelivery(
      @Value("${notification.simulate-failure:false}") boolean simulateFailure) {
    this.simulateFailure = simulateFailure;
  }

  @Override
  public void sendReviewReminder(ReminderNotification reminder) {
    if (delivered.contains(reminder.notificationId())) {
      LOGGER.info("Suppressing duplicate simulated notification notificationId={}", reminder.notificationId());
      return;
    }
    if (simulateFailure) {
      throw new IllegalStateException(
          "Simulated notification provider outage");
    }

    LOGGER.info(
        "Simulated review reminder: notificationId={}, processInstanceKey={}, dppResultId={}, transmissionOwnerId={}",
        reminder.notificationId(),
        reminder.processInstanceKey(),
        safeValue(reminder.dppResultId()),
        safeValue(reminder.transmissionOwnerId()));
    delivered.add(reminder.notificationId());
  }

  private String safeValue(String value) {
    return value == null || value.isBlank() ? "not-provided" : value;
  }
}
