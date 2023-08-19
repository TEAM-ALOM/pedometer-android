    package com.example.pedometer
    import android.content.Context
    import android.content.Intent
    import android.util.Log
    import androidx.health.connect.client.HealthConnectClient
    import androidx.health.connect.client.records.StepsRecord
    import androidx.health.connect.client.request.AggregateRequest
    import androidx.health.connect.client.time.TimeRangeFilter
    import androidx.work.Worker
    import androidx.work.WorkerParameters
    import com.example.pedometer.Model.StepsDatabase
    import com.example.pedometer.Model.StepsEntity
    import com.example.pedometer.repository.StepRepository
    import com.example.pedometer.repository.StepRepositoryImpl
    import kotlinx.coroutines.*
    import java.text.SimpleDateFormat
    import java.time.Instant
    import java.util.*

    @Suppress("DEPRECATION")
    class MyWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

        private val stepRepository: StepRepository by lazy {
            StepRepositoryImpl(
                StepsDatabase.getInstance(applicationContext).stepsDAO(),
                applicationContext
            )
        }

        override fun doWork(): Result {
            Log.d("MyWorker", "Do work")
            return try {
                val currentTime = Calendar.getInstance()
                currentTime.set(Calendar.MINUTE, 0)
                currentTime.set(Calendar.SECOND, 0)
                currentTime.set(Calendar.MILLISECOND, 0)
                val startTime = currentTime.timeInMillis
                val endTime = System.currentTimeMillis()

                CoroutineScope(Dispatchers.IO).launch {
                    val healthPermissionTool = HealthPermissionTool(applicationContext)

                    val sdkAvailable = healthPermissionTool.checkSdkStatusAndPromptForInstallation()

                    if (sdkAvailable) {
                        val healthConnectClient = HealthConnectClient.getOrCreate(applicationContext)

                        val response = healthConnectClient.aggregate(
                            AggregateRequest(
                                metrics = setOf(StepsRecord.COUNT_TOTAL),
                                timeRangeFilter = TimeRangeFilter.between(
                                    startTime = Instant.ofEpochMilli(startTime),
                                    endTime = Instant.ofEpochMilli(endTime)
                                )
                            )
                        )
                        val stepCount = response[StepsRecord.COUNT_TOTAL]

                        if (stepCount != null) {
                            val stepsEntity = StepsEntity(
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startTime),
                                todaySteps = stepCount.toInt(),
                                goalSteps = getGoalStepsFromRepository()
                            )

                            saveStepDataToRepository(stepsEntity)

                            // ForegroundService 호출하여 상태바 알림 띄우기
                            val serviceIntent = Intent(applicationContext, MyForegroundService::class.java)
                            serviceIntent.putExtra("stepsToday", stepsEntity.todaySteps)
                            serviceIntent.putExtra("stepsGoal", stepsEntity.goalSteps)
                            applicationContext.startService(serviceIntent)
                        }
                    }
                }

                Result.success()    // 작업 성공을 return
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure()
            }
        }

        private suspend fun getGoalStepsFromRepository(): Int {
            val stepsGoal = stepRepository.getStepsGoal().value
            val sharedPreferences = applicationContext.getSharedPreferences("stepsData", Context.MODE_PRIVATE)
            val storedStepsGoal = sharedPreferences.getInt("stepsGoal", 0)

            // 만약 LiveData에서 가져온 값이 0인 경우에는 SharedPreference 값으로 대체
            return if (stepsGoal != null && stepsGoal > 0) {
                Log.d("MyWorker", "stepsGoal: $stepsGoal")
                stepsGoal
            } else {
                Log.d("MyWorker", "Using stored stepsGoal: $storedStepsGoal")
                storedStepsGoal
            }
        }


        private suspend fun saveStepDataToRepository(stepsEntity: StepsEntity) {
            withContext(Dispatchers.IO) {
                stepRepository.saveStepData(stepsEntity)
            }
        }
    }
