package com.miso.planning.notification.application;

public record ReminderNotification(
    long processInstanceKey, String dppResultId, String transmissionOwnerId) {}
