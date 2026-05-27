package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
                }
            ).forEach { manager.createNotificationChannel(it) }
        }
    }

    fun showWakeUpAlarm(context: Context) {
        val notifId = NotificationIds.notifCounter++
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(context, notifId, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, NotificationIds.CHANNEL_WAKE_UP)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Good Morning, Teacher!")
            .setContentText("Time to rise and shine. Check today's schedule and prepare your lessons.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {}
    }

    fun showScheduleReminder(context: Context, subject: String, gradeClass: String, startTime: String, schoolName: String) {
        val notifId = NotificationIds.notifCounter++
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(context, notifId, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, NotificationIds.CHANNEL_REMINDER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Upcoming Class: $subject")
            .setContentText("$gradeClass at $startTime ${if (schoolName.isNotEmpty()) "· $schoolName" else ""}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Class: $gradeClass\nSubject: $subject\nTime: $startTime\nSchool: ${schoolName.ifEmpty { "N/A" }}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {}
    }

    fun showMissedSchedule(context: Context, subject: String, gradeClass: String, dayOfWeek: String) {
        val notifId = NotificationIds.notifCounter++
        val notification = NotificationCompat.Builder(context, NotificationIds.CHANNEL_MISSED)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Missed Schedule")
            .setContentText("$subject ($gradeClass) on $dayOfWeek was not marked complete.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {}
    }

    fun showUncompletedNotes(context: Context, count: Int) {
        val notifId = NotificationIds.notifCounter++
        val notification = NotificationCompat.Builder(context, NotificationIds.CHANNEL_NOTES)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Pending Lesson Notes")
            .setContentText("You have $count uncompleted lesson note(s) to finish.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {}
    }
}
