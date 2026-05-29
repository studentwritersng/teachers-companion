package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.createChannels(context)
        when (intent.action) {
            "com.example.action.WAKE_UP" -> {
                NotificationHelper.showWakeUpAlarm(context)
            }
            "com.example.action.CHECK_SCHEDULES" -> {
                AlarmScheduler.scheduleDailyWork(context)
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
}
