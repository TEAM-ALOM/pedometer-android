package com.example.pedometer.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.pedometer.Model.StepsDAO
import com.example.pedometer.StepSensorHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

@Suppress("UNREACHABLE_CODE", "DEPRECATION")
class StepRepositoryImpl(
        private val context: Context,
        private val stepsDAO: StepsDAO
) : StepRepository {
        private val stepSensorHelper = StepSensorHelper(context) // StepSensorHelper 인스턴스 생성
        private var _stepsToday = MutableLiveData<Int>()
        private var _stepsAvg = MutableLiveData<Int>()
        private var _stepsGoal = MutableLiveData<Int>()
        private var _date = MutableLiveData<Long>()

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
        override suspend fun getDate(): LiveData<Long> {
                val currentDate = System.currentTimeMillis()
                _date.value = currentDate
                return _date
        }



        override suspend fun updateStepsNow() {
                try {
                        val stepsTodayLiveData = stepSensorHelper.stepsToday
                        stepsTodayLiveData.observeForever(object : Observer<Int> {
                                override fun onChanged(value: Int) {
                                        _stepsToday.postValue(value) // LiveData에 값을 업데이트
                                        stepsTodayLiveData.removeObserver(this) // 옵저버 해제
                                }
                        })
                } catch (e: Exception) {
                        e.printStackTrace()
                }
        }


        override suspend fun updateStepsAverage() {
                _stepsAvg.postValue(0)
                CoroutineScope(Dispatchers.IO).launch {
                        // 현재 시간을 가져와서 endTime으로 설정
                        val endTime = Instant.now().toEpochMilli()
                        // 1주일 전의 시간을 가져와서 startTime으로 설정
                        val startTime = Instant.now().minusSeconds(7 * 24 * 60 * 60).toEpochMilli()

                        try {
                                // 룸 데이터베이스에서 해당 기간 동안의 걸음수 데이터 가져오기
                                val stepsFromDatabase = stepsDAO.getStepsBetweenDates(startTime, endTime)

                                val totalDays = 7
                                val totalStepsFromDatabase = stepsFromDatabase.sumBy { it.todaySteps ?: 0 } // 데이터가 없는 경우 0으로 처리

                                // 일주일간의 평균 걸음수 계산
                                val averageSteps = totalStepsFromDatabase.toFloat() / totalDays

                                // 업데이트된 stepsAvg를 화면에 표시
                                _stepsAvg.postValue(averageSteps.toInt()) // 라이브 데이터에 저장
                        } catch (e: Exception) {
                                // 걸음 수 데이터 읽기 실패 시 에러 처리
                                e.printStackTrace()
                        }
                }
        }
}
