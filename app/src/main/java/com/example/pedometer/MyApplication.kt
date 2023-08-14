package com.example.pedometer

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Constraints
import java.util.concurrent.TimeUnit


class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val repeatInterval = 1L
        val repeatIntervalTimeUnit = TimeUnit.HOURS
        val workerTag = "data_sync_worker" // 데이터 동기화 작업을 주기적으로 하고자 WorkerTag 설정

        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            MyWorker::class.java, repeatInterval, repeatIntervalTimeUnit
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            workerTag, ExistingPeriodicWorkPolicy.REPLACE, periodicWorkRequest
        )
    }
}