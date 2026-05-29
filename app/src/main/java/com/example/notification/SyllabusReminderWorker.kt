package com.example.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONArray

class SyllabusReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

        if (!prefs.getBoolean("syllabus_reminder_enabled", true)) {
            return Result.success()
        }

        val syllabusJson = prefs.getString("syllabus_cache", "[]") ?: "[]"
        val notesJson = prefs.getString("lesson_notes_cache", "[]") ?: "[]"

        val syllabusItems = try {
            val arr = JSONArray(syllabusJson)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SyllabusCacheItem(
                    subject = obj.optString("subject", ""),
                    gradeClass = obj.optString("gradeClass", ""),
                    topic = obj.optString("topic", ""),
                    week = obj.optInt("week", 0),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    schoolName = obj.optString("schoolName", "")
                )
            }
        } catch (_: Exception) { emptyList() }

        val lessonNotes = try {
            val arr = JSONArray(notesJson)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                NoteCacheItem(
                    subject = obj.optString("subject", ""),
                    gradeClass = obj.optString("gradeClass", ""),
                    topic = obj.optString("topic", "")
                )
            }
        } catch (_: Exception) { emptyList() }

        val uncompletedSyllabus = syllabusItems.filter { !it.isCompleted }

        val missingNotes = uncompletedSyllabus.filter { syllabusItem ->
            lessonNotes.none { note ->
                note.subject.equals(syllabusItem.subject, ignoreCase = true) &&
                note.gradeClass.equals(syllabusItem.gradeClass, ignoreCase = true) &&
                note.topic.equals(syllabusItem.topic, ignoreCase = true)
            }
        }

        val missingDescriptions = missingNotes.map { item ->
            val weekPart = if (item.week > 0) "Week ${item.week}: " else ""
            val schoolPart = if (item.schoolName.isNotEmpty()) " (${item.schoolName})" else ""
            "${item.subject} - $weekPart${item.topic}$schoolPart"
        }

        if (missingDescriptions.isNotEmpty()) {
            NotificationHelper.showSyllabusReminderNotification(applicationContext, missingDescriptions)
        }

        val uncompletedCount = syllabusItems.count { !it.isCompleted }
        prefs.edit().putInt("uncompleted_notes_count", uncompletedCount).apply()
        if (prefs.getBoolean("uncompleted_notes_reminder", true) && uncompletedCount > 0) {
            NotificationHelper.showUncompletedNotes(applicationContext, uncompletedCount)
        }

        return Result.success()
    }

    data class SyllabusCacheItem(
        val subject: String,
        val gradeClass: String,
        val topic: String,
        val week: Int,
        val isCompleted: Boolean,
        val schoolName: String
    )

    data class NoteCacheItem(
        val subject: String,
        val gradeClass: String,
        val topic: String
    )
}
