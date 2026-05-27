package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.example.action.WAKE_UP" -> {
                NotificationHelper.showWakeUpAlarm(context)
            }
            "com.example.action.CHECK_SCHEDULES" -> {
                checkSchedules(context)
            }
            "com.example.action.SCHEDULE_REMINDER" -> {
                val subject = intent.getStringExtra("subject") ?: ""
                val gradeClass = intent.getStringExtra("gradeClass") ?: ""
                val startTime = intent.getStringExtra("startTime") ?: ""
                val schoolName = intent.getStringExtra("schoolName") ?: ""
                NotificationHelper.showScheduleReminder(context, subject, gradeClass, startTime, schoolName)
            }
        }
    }

    private fun checkSchedules(context: Context) {
        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("missed_schedule_alerts", true)) return

        val timetableJson = prefs.getString("timetable_cache", "[]") ?: "[]"
        // Parse timetable items and check for missed ones
        // (inline JSON parse to avoid dependency)
        try {
            val orgJson = org.json.JSONArray(timetableJson)
            val today = getCurrentDay()
            val currentTime = getCurrentTime()
            for (i in 0 until orgJson.length()) {
                val item = orgJson.getJSONObject(i)
                val day = item.optString("dayOfWeek", "")
                val endTime = item.optString("endTime", "")
                val isCompleted = item.optBoolean("isCompleted", false)
                val subject = item.optString("subject", "")
                val gradeClass = item.optString("gradeClass", "")

                if (day == today && !isCompleted && endTime < currentTime) {
                    NotificationHelper.showMissedSchedule(context, subject, gradeClass, day)
                }
            }
        } catch (_: Exception) {}

        // Check uncompleted lesson notes
        val uncompletedCount = prefs.getInt("uncompleted_notes_count", 0)
        if (prefs.getBoolean("uncompleted_notes_reminder", true) && uncompletedCount > 0) {
            NotificationHelper.showUncompletedNotes(context, uncompletedCount)
        }

        // Re-schedule the daily wake-up alarm (in case it was missed)
        if (prefs.getBoolean("wake_up_alarm_enabled", false)) {
            val hour = prefs.getInt("wake_up_hour", 5)
            val minute = prefs.getInt("wake_up_minute", 30)
            AlarmScheduler.scheduleWakeUpAlarm(context, hour, minute)
        }
    }

    private fun getCurrentDay(): String {
        val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val cal = java.util.Calendar.getInstance()
        return days[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
    }

    private fun getCurrentTime(): String {
        val cal = java.util.Calendar.getInstance()
        return String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
    }
}
