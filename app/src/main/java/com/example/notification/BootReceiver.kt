package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            NotificationHelper.createChannels(context)
            AlarmScheduler.scheduleDailyWork(context)

            val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("wake_up_alarm_enabled", false)) {
                val hour = prefs.getInt("wake_up_hour", 5)
                val minute = prefs.getInt("wake_up_minute", 30)
                AlarmScheduler.scheduleWakeUpAlarm(context, hour, minute)
            }
            AlarmScheduler.scheduleClassReminders(context)
        }
    }
}
