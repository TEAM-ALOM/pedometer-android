package com.example.pedometer.repository

import android.content.Context
import android.icu.text.SimpleDateFormat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.pedometer.Model.StepsDatabase
import com.example.pedometer.Model.StepsEntity
import java.time.Instant
import java.util.*

class StepRepositoryImpl(//의존성 주입용
        private var context: Context
) : StepRepository {
        private val healthConnectClient = HealthConnectClient.getOrCreate(context)

        private var _stepsToday = MutableLiveData<Int>()
        private var _stepsAvg = MutableLiveData<Int>()
        private var _stepsGoal = MutableLiveData<Int>()
        private var _date = MutableLiveData<Int>()

        override suspend fun getStepsToday(): LiveData<Int> {
                updateStepsNow()
                return _stepsToday
        }

        override suspend fun getStepsAvg(): LiveData<Int> {
                updateStepsAverage()
                return _stepsAvg
        }
        override suspend fun getStepsGoal(): LiveData<Int> {
                val sharedPrefs = context.getSharedPreferences("stepsData", Context.MODE_PRIVATE)
                val stepsGoal = sharedPrefs.getInt("stepsGoal", 0)
                _stepsGoal.value = stepsGoal
                return _stepsGoal
        }
        override suspend fun getDate(): LiveData<Int> {

                _date.value =
                return _date
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
                val startTime = Calendar.getInstance()
                startTime.set(Calendar.HOUR_OF_DAY, 6)
                startTime.set(Calendar.MINUTE, 0)
                startTime.set(Calendar.SECOND, 0)
                startTime.set(Calendar.MILLISECOND, 0)
                val currentDayStart = startTime.timeInMillis
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
                                _stepsToday.postValue(it.toInt())//라이브 데이터에 저장

                                val date = Calendar.getInstance().time
                                val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                                val stepsEntity = StepsEntity(  // Room Database에 오늘의 날짜. 걸음수, 목표걸음수 저장
                                        date = formattedDate,
                                        todaySteps = it.toInt(),
                                        goalSteps = getStepsGoal().value
                                )

                                // Room 데이터베이스에 데이터 저장
                                val stepsDAO = StepsDatabase.getInstance(context)?.stepsDAO()
                                stepsDAO?.insert(stepsEntity)


                        }
                } catch (e: Exception) {
                        // 걸음 수 데이터 읽기 실패 시 에러 처리
                        e.printStackTrace()
                }
        }

        private suspend fun readStepsDataAvg() {
                // 현재 시간을 가져와서 endTime으로 설정
                val endTime = Instant.now()
                // 1주일 전의 시간을 가져와서 startTime으로 설정
                val startTime = Instant.now().minusSeconds(7 * 24 * 60 * 60)

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
                                _stepsAvg.postValue(averageSteps)//라이브 데이터에 저장

                        }
                } catch (e: Exception) {
                        // 걸음 수 데이터 읽기 실패 시 에러 처리
                        e.printStackTrace()
                        // 또는 다른 방식으로 로그 출력
                }
        }
}
