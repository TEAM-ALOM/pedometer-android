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
        scheduleDailyWork(this)
    }
    private fun scheduleDailyWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<MyWorker>(
            repeatInterval = 1, // 하루마다 호출
            repeatIntervalTimeUnit = TimeUnit.DAYS // 하루 단위로 설정
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_work",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
        Log.e("MA", "MyWorker 호출")
    }


    private fun calculateInitialDelay(): Long {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance()
        targetTime.set(Calendar.HOUR_OF_DAY, 23)
        targetTime.set(Calendar.MINUTE, 59)
        targetTime.set(Calendar.SECOND, 0)
        targetTime.set(Calendar.MILLISECOND, 0)

        if (currentTime.timeInMillis < targetTime.timeInMillis) {
            // 현재 시간이 타겟 시간보다 이전인 경우 바로 실행하도록 0을 반환
            return 0
        } else {
            // 현재 시간이 타겟 시간보다 이후인 경우 다음 날 타겟 시간까지의 시간 차이 반환
            val nextDayTargetTime = Calendar.getInstance()
            nextDayTargetTime.timeInMillis = targetTime.timeInMillis + TimeUnit.DAYS.toMillis(1)
            return nextDayTargetTime.timeInMillis - currentTime.timeInMillis
        }
    }

}
