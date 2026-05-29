package com.example.notification

import java.util.concurrent.atomic.AtomicInteger

data class NotificationPrefs(
    val wakeUpAlarmEnabled: Boolean = false,
    val wakeUpHour: Int = 5,
    val wakeUpMinute: Int = 30,
    val scheduleReminderEnabled: Boolean = true,
    val reminderMinutesBefore: Int = 15,
    val missedScheduleAlerts: Boolean = true,
    val uncompletedNotesReminder: Boolean = true,
    val dailyScheduleEnabled: Boolean = true,
    val dailyScheduleHour: Int = 6,
    val dailyScheduleMinute: Int = 0,
    val syllabusReminderEnabled: Boolean = true,
    val syllabusReminderHour: Int = 7,
    val syllabusReminderMinute: Int = 0
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
    UNCOMPLETED_NOTE,
    DAILY_SCHEDULE,
    SYLLABUS_REMINDER
}

object NotificationIds {
    const val CHANNEL_WAKE_UP = "wake_up_alarm"
    const val CHANNEL_REMINDER = "schedule_reminder"
    const val CHANNEL_MISSED = "missed_schedule"
    const val CHANNEL_NOTES = "uncompleted_notes"
    const val CHANNEL_DAILY_SCHEDULE = "daily_schedule"
    const val CHANNEL_SYLLABUS = "syllabus_reminder"
    const val WAKE_UP_REQUEST_CODE = 1001
    const val SCHEDULE_CHECK_REQUEST_CODE = 1002
    const val DAILY_SCHEDULE_NOTIFICATION_ID = 3001
    const val SYLLABUS_REMINDER_NOTIFICATION_ID = 3002
    const val WORK_DAILY_SCHEDULE = "work_daily_schedule"
    const val WORK_SYLLABUS_REMINDER = "work_syllabus_reminder"
    var notifCounter = AtomicInteger(4000)
}
