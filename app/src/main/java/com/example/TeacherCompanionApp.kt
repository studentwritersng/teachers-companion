package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper

class TeacherCompanionApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        AlarmScheduler.scheduleDailyWork(this)
    }
}
