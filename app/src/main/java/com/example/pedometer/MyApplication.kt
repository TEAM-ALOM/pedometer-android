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
        val stepSensorHelper = StepSensorHelper(this) // context를 전달하여 초기화
        stepSensorHelper.startListening()
        Log.e("MyApplication","stepsensor 활성화")

        // 추가: 백그라운드 작업 스케줄링 호출
        scheduleDailyWork(this)
    }
    private fun scheduleDailyWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<MyWorker>(
            repeatInterval = 1, // 1 시간마다 호출
            repeatIntervalTimeUnit = TimeUnit.HOURS // 시간 단위로 설정
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "hourly_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        Log.e("MyApplication", "MyWorker 호출")
    }


    private fun calculateInitialDelay(): Long {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance()
        targetTime.set(Calendar.HOUR_OF_DAY, 24)
        targetTime.set(Calendar.MINUTE, 0)
        targetTime.set(Calendar.SECOND, 0)
        targetTime.set(Calendar.MILLISECOND, 0)

        return targetTime.timeInMillis - currentTime.timeInMillis
    }
}
