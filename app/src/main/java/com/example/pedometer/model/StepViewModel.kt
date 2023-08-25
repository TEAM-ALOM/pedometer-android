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
            val date = Calendar.getInstance()

            val stepsToday = _stepsToday.value ?: 0
            val stepsGoal = stepRepository.getStepsGoal().value?:0

            val iconColor = calculateIconColor(stepsToday, stepsGoal)
            val eventDay = EventDay(date, iconColor)
            events.add(eventDay)

            calendar.setEvents(events)
        }
    }

    private fun calculateIconColor(stepsToday: Int, stepsGoal: Int): Int {
        val ratio = if (stepsGoal > 0) stepsToday.toFloat() / stepsGoal.toFloat() else 0f

        return when {
            ratio >= 1.0 -> R.drawable.bluestep
            ratio >= 0.5 -> R.drawable.yellowstep
            else -> R.drawable.redstep
        }
    }
}
