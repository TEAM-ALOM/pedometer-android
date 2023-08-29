package com.example.pedometer
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope

class MyWorker(context: Context, workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = coroutineScope {
        try {
            val stepSensorHelper = StepSensorHelper(applicationContext, this)
            stepSensorHelper.startListening()
            Log.e("MyWorker","센서 가동")
            val intent = Intent(applicationContext, MyForegroundService::class.java)
            applicationContext.startService(intent)
            Log.e("MyWorker","알림")
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

