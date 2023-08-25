package com.example.pedometer.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.pedometer.model.StepsDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

class StepRepositoryImpl(
        private val stepsDAO: StepsDAO
) : StepRepository {

        override suspend fun getStepsToday(): LiveData<Int> {
                updateStepsNow()
                return stepsToday
        }

        override suspend fun getStepsAvg(): LiveData<Int> {
                updateStepsAverage()
                return stepsAvg
        }

        override suspend fun getStepsGoal(): LiveData<Int> {
                updateStepsGoal()
                return stepsGoal
        }

        private fun getCurrentDate(): String {
                val currentTimeMillis = System.currentTimeMillis()
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                return dateFormatter.format(Date(currentTimeMillis))
        }

        override suspend fun getDate(): Long {
                return System.currentTimeMillis()
        }
        private suspend fun updateStepsGoal() {
                withContext(Dispatchers.IO) {
                        try {
                                val currentDate = getCurrentDate()
                                val stepsEntity = stepsDAO.getByDate(currentDate)
                                stepsGoal.postValue(stepsEntity?.goalSteps ?: 0)
                        } catch (e: Exception) {
                                e.printStackTrace()
                        }
                }
        }
        override suspend fun updateStepsNow() {
                withContext(Dispatchers.IO) {
                        try {
                                val currentDate = getCurrentDate()
                                val stepsEntity = stepsDAO.getByDate(currentDate)
                                stepsToday.postValue(stepsEntity?.todaySteps ?: 0)
                        } catch (e: Exception) {
                                e.printStackTrace()
                        }
                }
        }

        override suspend fun updateStepsAverage() {
                withContext(Dispatchers.IO) {
                        try {
                                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val endDate = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                                val startDate = LocalDate.now().minusDays(6).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                                val stepsFromDatabase = stepsDAO.getStepsBetweenDates(
                                        dateFormatter.format(Date(startDate)),
                                        dateFormatter.format(Date(endDate))
                                )
                                val totalStepsFromDatabase = stepsFromDatabase.sumOf { it.todaySteps ?: 0 }
                                val totalDays = 7
                                val averageSteps = totalStepsFromDatabase.toFloat() / totalDays
                                stepsAvg.postValue(averageSteps.toInt())
                        } catch (e: Exception) {
                                e.printStackTrace()
                        }
                }
        }



        private val stepsToday = MutableLiveData<Int>()
        private val stepsAvg = MutableLiveData<Int>()
        private val stepsGoal = MutableLiveData<Int>()
}
