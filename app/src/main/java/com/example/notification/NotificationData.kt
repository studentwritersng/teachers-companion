package com.example.notification

data class NotificationPrefs(
    val wakeUpAlarmEnabled: Boolean = false,
    val wakeUpHour: Int = 5,
    val wakeUpMinute: Int = 30,
    val scheduleReminderEnabled: Boolean = true,
    val reminderMinutesBefore: Int = 15,
    val missedScheduleAlerts: Boolean = true,
    val uncompletedNotesReminder: Boolean = true
)

data class AppNotification(
    val id: Int,
    val title: String,
    val body: String,
    val type: NotificationType,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

enum class NotificationType {
    WAKE_UP,
    SCHEDULE_REMINDER,
    MISSED_SCHEDULE,
    UNCOMPLETED_NOTE
}

object NotificationIds {
    const val CHANNEL_WAKE_UP = "wake_up_alarm"
    const val CHANNEL_REMINDER = "schedule_reminder"
    const val CHANNEL_MISSED = "missed_schedule"
    const val CHANNEL_NOTES = "uncompleted_notes"
    const val WAKE_UP_REQUEST_CODE = 1001
    const val SCHEDULE_CHECK_REQUEST_CODE = 1002
    var notifCounter = 2000
}
