package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            listOf(
                NotificationChannel(
                    NotificationIds.CHANNEL_WAKE_UP,
                    "Wake-up Alarm",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Daily wake-up alarm for teachers"
                    enableVibration(true)
                    setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI, null)
                },
                NotificationChannel(
                    NotificationIds.CHANNEL_REMINDER,
                    "Schedule Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders before scheduled classes"
                    enableVibration(true)
                },
                NotificationChannel(
                    NotificationIds.CHANNEL_MISSED,
                    "Missed Schedules",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts for missed or past-due schedules"
                    enableVibration(true)
                },
                NotificationChannel(
                    NotificationIds.CHANNEL_NOTES,
                    "Uncompleted Notes",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Reminders about pending lesson notes"
                    enableVibration(false)
                },
                NotificationChannel(
                    NotificationIds.CHANNEL_DAILY_SCHEDULE,
                    "Daily Teaching Schedule",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Morning summary of your teaching day ahead"
                    enableVibration(true)
                    setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null)
                },
                NotificationChannel(
                    NotificationIds.CHANNEL_SYLLABUS,
                    "Syllabus Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for syllabus topics without lesson notes"
                    enableVibration(true)
                }
            ).forEach { manager.createNotificationChannel(it) }
        }
    }

    private fun launchIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showWakeUpAlarm(context: Context) {
        val notifId = NotificationIds.notifCounter.getAndIncrement()
        val notification = NotificationCompat.Builder(context, NotificationIds.CHANNEL_WAKE_UP)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Good Morning, Teacher!")
            .setContentText("Time to rise and shine. Check today's schedule and prepare your lessons.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(launchIntent(context, notifId))
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {}
    }

    fun showScheduleReminder(context: Context, subject: String, gradeClass: String, startTime: String, schoolName: String) {
        val notifId = NotificationIds.notifCounter.getAndIncrement()
        val notification = NotificationCompat.Builder(context, NotificationIds.CHANNEL_REMINDER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Upcoming Class: $subject")
            .setContentText("$gradeClass at $startTime ${if (schoolName.isNotEmpty()) "· $schoolName" else ""}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Class: $gradeClass\nSubject: $subject\nTime: $startTime\nSchool: ${schoolName.ifEmpty { "N/A" }}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(launchIntent(context, notifId))
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {}
    }

    fun showMissedSchedule(context: Context, subject: String, gradeClass: String, dayOfWeek: String) {
        val notifId = NotificationIds.notifCounter.getAndIncrement()
        val notification = NotificationCompat.Builder(context, NotificationIds.CHANNEL_MISSED)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Missed Schedule")
            .setContentText("$subject ($gradeClass) on $dayOfWeek was not marked complete.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(launchIntent(context, notifId))
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {}
    }

    fun showUncompletedNotes(context: Context, count: Int) {
        val notifId = NotificationIds.notifCounter.getAndIncrement()
        val notification = NotificationCompat.Builder(context, NotificationIds.CHANNEL_NOTES)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Pending Lesson Notes")
            .setContentText("You have $count uncompleted lesson note(s) to finish.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(launchIntent(context, notifId))
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {}
    }

    fun showDailyScheduleNotification(
        context: Context,
        schools: List<String>,
        subjects: List<String>,
        periodCount: Int,
        totalHours: String,
        scheduleLines: List<String>
    ) {
        val bigText = buildString {
            if (schools.isNotEmpty()) {
                append("Schools: ${schools.joinToString(", ")}\n")
            }
            append("Periods: $periodCount\n")
            append("Total Hours: $totalHours\n")
            append("Subjects: ${subjects.joinToString(", ")}\n\n")
            scheduleLines.forEach { appendLine(it) }
        }

        val contentText = if (periodCount > 0) {
            "$periodCount period(s) at ${schools.joinToString(", ")} · $totalHours"
        } else {
            "No classes scheduled for today"
        }

        val notification = NotificationCompat.Builder(context, NotificationIds.CHANNEL_DAILY_SCHEDULE)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("Today's Teaching Schedule")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(launchIntent(context, NotificationIds.DAILY_SCHEDULE_NOTIFICATION_ID))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(NotificationIds.DAILY_SCHEDULE_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }

    fun showSyllabusReminderNotification(
        context: Context,
        missingNotes: List<String>
    ) {
        if (missingNotes.isEmpty()) return

        val count = missingNotes.size
        val bigText = buildString {
            append("$count topic(s) in your scheme of work still need lesson notes:\n\n")
            missingNotes.take(8).forEach { appendLine("• $it") }
            if (count > 8) append("\n... and ${count - 8} more")
        }

        val notification = NotificationCompat.Builder(context, NotificationIds.CHANNEL_SYLLABUS)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Lesson Notes Needed ($count)")
            .setContentText("$count syllabus topic(s) still need lesson notes")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(launchIntent(context, NotificationIds.SYLLABUS_REMINDER_NOTIFICATION_ID))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(NotificationIds.SYLLABUS_REMINDER_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }
}
