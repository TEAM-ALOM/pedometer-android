package com.example.pedometer

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.lifecycle.LiveData
import androidx.work.*
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.example.pedometer.Model.StepsDatabase
import com.example.pedometer.Model.StepsEntity
import java.util.*

class MyWorker(context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {

    // 주기적인 백그라운드 작업을 정의하고 수행하기 위한 공간
    override fun doWork(): Result {     // 수행될 작업 구현
        // 데이터 동기화 작업 수행

        val date = Calendar.getInstance().time
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        val stepsEntity = StepsEntity(
            date = formattedDate,
            todaySteps = getStepsToday().value,
            goalSteps = getStepsGoal().value
        )

        // Room Database에 데이터 저장
        val stepsDAO = StepsDatabase.getInstance(applicationContext)?.stepsDAO()
        stepsDAO?.insert(stepsEntity)

        return Result.success()     // 작업 성공을 return


    }

    private fun getStepsToday(): LiveData<Int> {
        // 현재 걸음 수를 얻는 코드 작성
        // LiveData<Int>를 반환

    }

    private fun getStepsGoal(): LiveData<Int> {
        // 목표 걸음 수를 얻는 코드 작성
        // LiveData<Int>를 반환

    }

    fun scheduleDailyWork(context: Context) {
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

    fun calculateInitialDelay(): Long {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance()
        targetTime.set(Calendar.HOUR_OF_DAY, 23)
        targetTime.set(Calendar.MINUTE, 59)
        targetTime.set(Calendar.SECOND, 0)
        targetTime.set(Calendar.MILLISECOND, 0)

        return targetTime.timeInMillis - currentTime.timeInMillis
    }
}