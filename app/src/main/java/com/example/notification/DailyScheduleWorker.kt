package com.example.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONArray
import java.util.Calendar

class DailyScheduleWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

        if (!prefs.getBoolean("daily_schedule_enabled", true)) {
            return Result.success()
        }

        val timetableJson = prefs.getString("timetable_cache", "[]") ?: "[]"
        val today = getCurrentDay()

        val todayItems = try {
            val arr = JSONArray(timetableJson)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                if (obj.optString("dayOfWeek") == today) {
                    TimetableCacheItem(
                        dayOfWeek = obj.optString("dayOfWeek", ""),
                        startTime = obj.optString("startTime", ""),
                        endTime = obj.optString("endTime", ""),
                        subject = obj.optString("subject", ""),
                        gradeClass = obj.optString("gradeClass", ""),
                        schoolName = obj.optString("schoolName", ""),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                } else null
            }.sortedBy { it.startTime }
        } catch (_: Exception) { emptyList() }

        if (todayItems.isEmpty()) {
            NotificationHelper.showDailyScheduleNotification(
                applicationContext,
                schools = emptyList(),
                subjects = emptyList(),
                periodCount = 0,
                totalHours = "0h 0m",
                scheduleLines = listOf("No classes scheduled for today. Enjoy your free day!")
            )
            return Result.success()
        }

        val schools = todayItems.map { it.schoolName }.filter { it.isNotEmpty() }.distinct()
        val subjects = todayItems.map { it.subject }.distinct()
        val periodCount = todayItems.size
        val totalMinutes = todayItems.sumOf { calculateDurationMinutes(it.startTime, it.endTime) }
        val totalHours = "${totalMinutes / 60}h ${totalMinutes % 60}m"

        val scheduleLines = todayItems.mapIndexed { index, item ->
            val schoolPart = if (item.schoolName.isNotEmpty()) " @ ${item.schoolName}" else ""
            "${index + 1}. ${item.subject} (${item.gradeClass})$schoolPart — ${item.startTime}-${item.endTime}"
        }

        NotificationHelper.showDailyScheduleNotification(
            applicationContext,
            schools = schools,
            subjects = subjects,
            periodCount = periodCount,
            totalHours = totalHours,
            scheduleLines = scheduleLines
        )

        checkMissedSchedules(prefs, today, todayItems)

        return Result.success()
    }

    private fun checkMissedSchedules(prefs: android.content.SharedPreferences, today: String, todayItems: List<TimetableCacheItem>) {
        if (!prefs.getBoolean("missed_schedule_alerts", true)) return
        val currentMinutes = parseTimeToMinutes(getCurrentTime())
        for (item in todayItems) {
            if (!item.isCompleted && parseTimeToMinutes(item.endTime) < currentMinutes) {
                NotificationHelper.showMissedSchedule(applicationContext, item.subject, item.gradeClass, today)
            }
        }
    }

    private fun getCurrentDay(): String {
        val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val cal = Calendar.getInstance()
        return days[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }

    private fun getCurrentTime(): String {
        val cal = Calendar.getInstance()
        return String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    private fun calculateDurationMinutes(startTime: String, endTime: String): Int {
        return try {
            val startMin = parseTimeToMinutes(startTime)
            val endMin = parseTimeToMinutes(endTime)
            if (endMin > startMin) endMin - startMin else 0
        } catch (_: Exception) { 0 }
    }

    private fun parseTimeToMinutes(time: String): Int {
        val (h, m) = time.split(":").map { it.toInt() }
        return h * 60 + m
    }

    data class TimetableCacheItem(
        val dayOfWeek: String,
        val startTime: String,
        val endTime: String,
        val subject: String,
        val gradeClass: String,
        val schoolName: String,
        val isCompleted: Boolean
    )
}
