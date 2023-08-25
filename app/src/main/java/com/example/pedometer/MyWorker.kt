package com.example.pedometer

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MyWorker(context: Context, workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {

    // 주기적인 백그라운드 작업을 정의하고 수행하기 위한 공간
    override suspend fun doWork(): Result {
        return try {
            val stepSensorHelper = StepSensorHelper(applicationContext)
            stepSensorHelper.startListening() // StepSensorHelper 호출/ 걸음수 측 정 및 데이터 베이스에 저장

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}