package com.example.pedometer

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleDailyWork(this)
    }

    private fun scheduleDailyWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequest.Builder(
            MyWorker::class.java,
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_work",
            ExistingPeriodicWorkPolicy.REPLACE, // REPLACE로 변경
            workRequest
        )
        Log.e("MA", "MyWorker 호출")
    }
}
