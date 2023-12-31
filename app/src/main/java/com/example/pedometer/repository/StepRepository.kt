package com.example.pedometer.repository

import androidx.lifecycle.LiveData
import com.example.pedometer.model.StepsEntity

interface StepRepository {
    suspend fun getStepsToday(): LiveData<Int>
    suspend fun getStepsAvg(): LiveData<Int>
    suspend fun getDate(): Long
    suspend fun getStepsGoal(): LiveData<Int>
    suspend fun updateStepsNow()
    suspend fun updateStepsAverage()
    suspend fun getAllSteps(): List<StepsEntity>
}
