package com.miso.planning.notification.application;

/**
 * Boundary for notification delivery. Implementations must be idempotent because Camunda job
 * processing is at-least-once.
 */
public interface NotificationDelivery {

  void sendReviewReminder(ReminderNotification reminder);
}
