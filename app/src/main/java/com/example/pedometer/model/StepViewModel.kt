package com.example.pedometer.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.example.pedometer.R
import com.example.pedometer.repository.StepRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StepViewModel(private val stepRepository: StepRepository) : ViewModel() {
    private val _stepsToday: MutableLiveData<Int> = MutableLiveData()
    private val _stepsAvg: MutableLiveData<Int> = MutableLiveData()
    private val _stepsGoal: MutableLiveData<Int> = MutableLiveData()

    val stepsToday: LiveData<Int>
        get() = _stepsToday
    val stepsAvg: LiveData<Int>
        get() = _stepsAvg

    fun updateStepsNow() {
        viewModelScope.launch {
            val stepsTodayValue = stepRepository.getStepsToday().value?:0
            _stepsToday.postValue(stepsTodayValue)
        }
    }

    fun updateStepsAverage() {
        viewModelScope.launch {
            val stepsAvgValue = stepRepository.getStepsAvg().value?:0
            _stepsAvg.postValue(stepsAvgValue)
        }
    }

    fun updateCalendarIcons(calendar: CalendarView) {
        viewModelScope.launch {
            val events: MutableList<EventDay> = ArrayList()
            val stepsEntities = stepRepository.getAllSteps()    // 모든 StepsEntity 가져오기

            for(stepsEntity in stepsEntities){
                val iconColor = calculateIconColor(stepsEntity.todaySteps!!, stepsEntity.goalSteps!!)
                // 강제로 non-nullable로 변환. 만약 값이 없었던 경우 런타임에서 NullPointException이 발생할 수 있음.
                val date = Calendar.getInstance().apply {
                    timeInMillis = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stepsEntity.date)?.time ?: 0
                }
                val eventDay = EventDay(date, iconColor)
                events.add(eventDay)
            }
            calendar.setEvents(events)
        }
    }

    private fun calculateIconColor(stepsToday: Int, stepsGoal: Int): Int {
        if (stepsToday == null || stepsGoal == null) {
            // 값이 없을 경우에 대한 처리
        }
        val ratio = if (stepsGoal > 0) stepsToday.toFloat() / stepsGoal.toFloat() else 0f

        return when {
            ratio >= 1.0 -> R.drawable.bluestep
            ratio >= 0.5 -> R.drawable.yellowstep
            else -> R.drawable.redstep
        }
    }
}
