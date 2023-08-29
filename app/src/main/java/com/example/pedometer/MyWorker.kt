package com.example.pedometer
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope

class MyWorker(context: Context, workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = coroutineScope {
        try {
            val stepSensorHelper = StepSensorHelper(applicationContext, this)
            stepSensorHelper.stopListening()
            stepSensorHelper.startListening()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

