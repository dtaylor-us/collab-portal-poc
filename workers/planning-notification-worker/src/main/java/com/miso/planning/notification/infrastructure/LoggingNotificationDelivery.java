package com.miso.planning.notification.infrastructure;

import com.miso.planning.notification.application.NotificationDelivery;
import com.miso.planning.notification.application.ReminderNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationDelivery implements NotificationDelivery {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingNotificationDelivery.class);

  @Override
  public void sendReviewReminder(ReminderNotification reminder) {
    LOGGER.info(
        "Simulated review reminder: processInstanceKey={}, dppResultId={}, transmissionOwnerId={}",
        reminder.processInstanceKey(),
        safeValue(reminder.dppResultId()),
        safeValue(reminder.transmissionOwnerId()));
  }

  private String safeValue(String value) {
    return value == null || value.isBlank() ? "not-provided" : value;
  }
}
