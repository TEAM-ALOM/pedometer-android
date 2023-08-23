package com.example.pedometer

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.*
import com.example.pedometer.Model.StepsDatabase
import com.example.pedometer.Model.StepsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

class MyWorker(context: Context, workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {

    // 주기적인 백그라운드 작업을 정의하고 수행하기 위한 공간
    override suspend fun doWork(): Result { // 코루틴을 사용하여 suspend 함수로 변경(백그라운드에서 헬스커넥트 사용하기 위함)
        return try {
            val date = Calendar.getInstance().time
            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            val stepsEntity = StepsEntity(
                date = formattedDate,
                todaySteps = getStepsToday(applicationContext).value,
                goalSteps = getStepsGoal(applicationContext).value
            )

            // 백그라운드 스레드에서 데이터베이스에 접근하여 저장
            val stepsDAO = StepsDatabase.getInstance(applicationContext).stepsDAO()
            CoroutineScope(Dispatchers.IO).launch {
                stepsDAO.insert(stepsEntity)
            }

            Result.success() // 작업 성공을 return
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun getStepsToday(applicationContext: Context): LiveData<Int> {
        val healthConnectClient = HealthConnectClient.getOrCreate(applicationContext)
        val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))
        val granted = healthConnectClient.permissionController.getGrantedPermissions()

        if (granted.containsAll(permissions)) {
            val startTime = Calendar.getInstance()
            startTime.set(Calendar.HOUR_OF_DAY, 0)
            startTime.set(Calendar.MINUTE, 0)
            startTime.set(Calendar.SECOND, 0)
            startTime.set(Calendar.MILLISECOND, 0)
            val currentDayStart = startTime.timeInMillis
            val endTime = Instant.now().toEpochMilli()

            try {
                val response = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(
                            startTime = Instant.ofEpochMilli(currentDayStart),
                            endTime = Instant.ofEpochMilli(endTime)
                        )
                    )
                )
                val stepCount = response[StepsRecord.COUNT_TOTAL]
                return MutableLiveData(stepCount?.toInt() ?: 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return MutableLiveData(0)
    }

    private fun getStepsGoal(applicationContext: Context): LiveData<Int> {
        val sharedPrefs = applicationContext.getSharedPreferences("stepsData", Context.MODE_PRIVATE)
        val stepsGoal = sharedPrefs.getInt("stepsGoal", 0)
        return MutableLiveData(stepsGoal)
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