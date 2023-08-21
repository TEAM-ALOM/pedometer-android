package com.example.pedometer

import android.app.Application
import android.content.Context
import android.icu.util.Calendar
import androidx.work.*
import java.util.concurrent.TimeUnit


class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val repeatInterval = 1L
        val repeatIntervalTimeUnit = TimeUnit.DAYS
        val workerTag = "data_sync_worker"

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            MyWorker::class.java, repeatInterval, repeatIntervalTimeUnit
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            workerTag, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest
        )

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
    }

    private fun calculateInitialDelay(): Long {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance()
        targetTime.set(Calendar.HOUR_OF_DAY, 23)
        targetTime.set(Calendar.MINUTE, 59)
        targetTime.set(Calendar.SECOND, 0)
        targetTime.set(Calendar.MILLISECOND, 0)

        return targetTime.timeInMillis - currentTime.timeInMillis
    }
}
