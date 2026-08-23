package com.miso.planning.notification.application;

public record ReminderNotification(
    String notificationId, long processInstanceKey, String dppResultId, String transmissionOwnerId) {}
