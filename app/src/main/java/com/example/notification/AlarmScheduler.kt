package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONArray
import java.util.Calendar
import java.util.concurrent.TimeUnit

object AlarmScheduler {

    private const val CLASS_REMINDER_PREFIX = 5000

    fun scheduleDailyWork(context: Context) {
        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val dailyEnabled = prefs.getBoolean("daily_schedule_enabled", true)
        val syllabusEnabled = prefs.getBoolean("syllabus_reminder_enabled", true)

        if (dailyEnabled) {
            val dailyHour = prefs.getInt("daily_schedule_hour", 6)
            val dailyMinute = prefs.getInt("daily_schedule_minute", 0)
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, dailyHour)
                set(Calendar.MINUTE, dailyMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
            }
            val initialDelay = target.timeInMillis - now.timeInMillis

            val dailyWork = PeriodicWorkRequestBuilder<DailyScheduleWorker>(
                24, TimeUnit.HOURS,
                30, TimeUnit.MINUTES
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NotificationIds.WORK_DAILY_SCHEDULE,
                ExistingPeriodicWorkPolicy.REPLACE,
                dailyWork
            )
        } else {
            WorkManager.getInstance(context)
                .cancelUniqueWork(NotificationIds.WORK_DAILY_SCHEDULE)
        }

        if (syllabusEnabled) {
            val syllabusHour = prefs.getInt("syllabus_reminder_hour", 7)
            val syllabusMinute = prefs.getInt("syllabus_reminder_minute", 0)
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, syllabusHour)
                set(Calendar.MINUTE, syllabusMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
            }
            val initialDelay = target.timeInMillis - now.timeInMillis

            val syllabusWork = PeriodicWorkRequestBuilder<SyllabusReminderWorker>(
                24, TimeUnit.HOURS,
                30, TimeUnit.MINUTES
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NotificationIds.WORK_SYLLABUS_REMINDER,
                ExistingPeriodicWorkPolicy.REPLACE,
                syllabusWork
            )
        } else {
            WorkManager.getInstance(context)
                .cancelUniqueWork(NotificationIds.WORK_SYLLABUS_REMINDER)
        }

        scheduleClassReminders(context)
    }

    fun scheduleClassReminders(context: Context) {
        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("schedule_reminder_enabled", true)
        val minutesBefore = prefs.getInt("reminder_minutes_before", 15)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        cancelClassReminders(context)

        if (!enabled) return

        val timetableJson = prefs.getString("timetable_cache", "[]") ?: "[]"
        val today = getCurrentDay()
        val now = Calendar.getInstance()

        try {
            val arr = JSONArray(timetableJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val day = obj.optString("dayOfWeek", "")
                if (day != today) continue

                val startTime = obj.optString("startTime", "")
                val subject = obj.optString("subject", "")
                val gradeClass = obj.optString("gradeClass", "")
                val schoolName = obj.optString("schoolName", "")

                val (hour, minute) = try {
                    val parts = startTime.split(":")
                    Pair(parts[0].toInt(), parts[1].toInt())
                } catch (_: Exception) { continue }

                val reminderTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MINUTE, -minutesBefore)
                }

                if (reminderTime.after(now)) {
                    val intent = Intent(context, NotificationReceiver::class.java).apply {
                        action = "com.example.action.SCHEDULE_REMINDER"
                        putExtra("subject", subject)
                        putExtra("gradeClass", gradeClass)
                        putExtra("startTime", startTime)
                        putExtra("schoolName", schoolName)
                    }
                    val requestCode = CLASS_REMINDER_PREFIX + i
                    val pendingIntent = PendingIntent.getBroadcast(
                        context, requestCode, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime.timeInMillis, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime.timeInMillis, pendingIntent)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun cancelClassReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0 until 50) {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = "com.example.action.SCHEDULE_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, CLASS_REMINDER_PREFIX + i, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    fun scheduleWakeUpAlarm(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.example.action.WAKE_UP"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationIds.WAKE_UP_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelWakeUpAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.example.action.WAKE_UP"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationIds.WAKE_UP_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleScheduleCheck(context: Context) {
        scheduleDailyWork(context)
    }

    fun cancelScheduleCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NotificationIds.WORK_DAILY_SCHEDULE)
        WorkManager.getInstance(context).cancelUniqueWork(NotificationIds.WORK_SYLLABUS_REMINDER)
        cancelClassReminders(context)
    }

    private fun getCurrentDay(): String {
        val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val cal = Calendar.getInstance()
        return days[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }
}
