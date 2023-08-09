package com.example.pedometer.repository

import android.content.SharedPreferences
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.time.Instant
import java.time.temporal.ChronoUnit

class StepRepositoryImpl(
        private val sharedPreferences: SharedPreferences,
        private val healthConnectClient: HealthConnectClient
) : StepRepository {

        private val _stepsToday = MutableLiveData<Int>()
        private val _stepsAvg = MutableLiveData<Int>()

        override suspend fun getStepsToday(): LiveData<Int> {
                Log.e("StepReImpl", "오늘 걸음수 받아오기 성공")
                return MutableLiveData(sharedPreferences.getInt("stepsToday", 0))
        }

        override suspend fun getStepsAvg(): LiveData<Int> {
                Log.e("StepReImpl", "일주일 평균 걸음수 받아오기 성공")
                return MutableLiveData(sharedPreferences.getInt("stepsAvg", 0))
        }

        override suspend fun updateStepsNow() {
                // Health Connect SDK 사용하여 현재 걸음 수 업데이트
                val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))

                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                if (granted.containsAll(permissions)) {
                        readStepsDataToday()
                }

        }

        override suspend fun updateStepsAverage() {
                // Health Connect SDK 사용하여 평균 걸음 수 업데이트
                val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))

                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                if (granted.containsAll(permissions)) {
                        readStepsDataAvg()
                }
        }

        private suspend fun readStepsDataToday() {
                // 현재 시간을 가져와서 endTime으로 설정
                val endTime = Instant.now().toEpochMilli()
                // 당일 00시를 startTime으로 설정
                val currentDayStart = Instant.now().truncatedTo(ChronoUnit.DAYS).toEpochMilli()

                try {
                        // 걸음 수 데이터 읽기
                        val response = healthConnectClient.aggregate(
                                AggregateRequest(
                                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                                        timeRangeFilter = TimeRangeFilter.between(
                                                startTime = Instant.ofEpochMilli(currentDayStart),
                                                endTime = Instant.ofEpochMilli(endTime)
                                        )
                                )
                        )
                        val stepCount = response[StepsRecord.COUNT_TOTAL] as Long?
                        stepCount?.let {
                                sharedPreferences.edit().putInt("stepsToday", it.toInt()).apply()
                                _stepsToday.postValue(it.toInt())//라이브 데이터에 저장
                        }
                } catch (e: Exception) {
                        // 걸음 수 데이터 읽기 실패 시 에러 처리
                        e.printStackTrace()
                        // 또는 다른 방식으로 로그 출력
                        Log.e("StepRepositoryImpl", "걸음 수 데이터 읽기 실패: ${e.message}")
                }
        }

        private suspend fun readStepsDataAvg() {
                // 현재 시간을 가져와서 endTime으로 설정
                val endTime = Instant.now()
                // 1주일 전의 시간을 가져와서 startTime으로 설정
                val startTime = endTime.minusSeconds(7 * 24 * 60 * 60)

                try {
                        // 걸음 수 데이터 읽기
                        val response = healthConnectClient.aggregate(
                                AggregateRequest(
                                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                                )
                        )
                        val stepCount = response[StepsRecord.COUNT_TOTAL] as Long?

                        // 일주일간의 평균 걸음수 계산
                        val averageSteps = stepCount?.toFloat()?.div(7)?.toInt() ?: 0

                        // 업데이트된 stepsNow와 stepsAvg를 화면에 표시
                        stepCount?.let {
                                sharedPreferences.edit().putInt("stepsAvg", it.toInt()).apply()
                                _stepsAvg.postValue(it.toInt())//라이브 데이터에 저장
                        }
                } catch (e: Exception) {
                        // 걸음 수 데이터 읽기 실패 시 에러 처리
                        e.printStackTrace()
                        // 또는 다른 방식으로 로그 출력
                        Log.e("StepRepositoryImpl", "걸음 수 데이터 읽기 실패: ${e.message}")
                }
        }
}
