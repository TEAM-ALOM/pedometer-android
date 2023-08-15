package com.example.pedometer.repository

import androidx.lifecycle.LiveData

interface StepRepository {//걸음수 레포지토리
    suspend fun getStepsToday(): LiveData<Int>
    suspend fun getStepsAvg(): LiveData<Int>
    suspend fun getStepsGoal(): LiveData<Int>
    suspend fun updateStepsNow()
    suspend fun updateStepsAverage()
}
