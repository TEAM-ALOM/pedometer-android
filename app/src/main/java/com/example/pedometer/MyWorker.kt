package com.example.pedometer

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class MyWorker(context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {

    override fun doWork(): Result {
        return try {
            val stepSensorHelper = StepSensorHelper(applicationContext)
            stepSensorHelper.stopListening()
            stepSensorHelper.startListening()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
