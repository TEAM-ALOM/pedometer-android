package com.example.pedometer

import android.app.Application
import android.content.Context
import android.icu.util.Calendar
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit


class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.e("MyApplication","Myapllication 활성화")
        // 추가: 백그라운드 작업 스케줄링 호출
        scheduleDailyWork(this)
    }
    private fun scheduleDailyWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<MyWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        Log.e("MyApplication","MyWorker 호출")
    }

    private fun calculateInitialDelay(): Long {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance()
        targetTime.set(Calendar.HOUR_OF_DAY, 20)
        targetTime.set(Calendar.MINUTE, 56)
        targetTime.set(Calendar.SECOND, 0)
        targetTime.set(Calendar.MILLISECOND, 0)

        return targetTime.timeInMillis - currentTime.timeInMillis
    }
}
