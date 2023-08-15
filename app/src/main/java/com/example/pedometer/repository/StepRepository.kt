package com.example.pedometer.repository

import androidx.lifecycle.LiveData
import com.example.pedometer.Model.StepsEntity

interface StepRepository {

    //걸음수 레포지토리
    suspend fun getStepsToday(): LiveData<Int>//오늘 걸음수
    suspend fun getStepsAvg(): LiveData<Int>//평균 걸음수
    suspend fun getStepsGoal(): LiveData<Int>//목표 걸음수
    suspend fun updateStepsNow()//오늘 걸음수 업데이트
    suspend fun updateStepsAverage()//평균 걸음수 업데이트
    suspend fun saveStepData(stepsEntity: StepsEntity)//날짜별 데이터 저장
    suspend fun getByDate(date: String): StepsEntity?//날짜별 걸음수 가져오기
}
