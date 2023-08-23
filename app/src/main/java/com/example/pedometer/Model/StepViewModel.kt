package com.example.pedometer.Model
import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.example.pedometer.repository.StepRepository
import kotlinx.coroutines.launch

class StepViewModel(private val stepRepository: StepRepository) : ViewModel() {//걸음수 뷰모델

    private val _stepsToday: MutableLiveData<Int> by lazy { MutableLiveData() }
    private val _stepsAvg: MutableLiveData<Int> by lazy { MutableLiveData() }
    private val _stepsGoal: MutableLiveData<Int> by lazy { MutableLiveData() }

    val stepsToday: LiveData<Int>
        get() = _stepsToday
    val stepsAvg: LiveData<Int>
        get() = _stepsAvg
    val stepsGoal: LiveData<Int>
        get() = _stepsGoal

    fun updateStepsNow() {
        viewModelScope.launch {
            _stepsToday.value = stepRepository.getStepsToday().value ?: 0
        }
    }

    fun updateStepsAverage() {
        viewModelScope.launch {
            _stepsAvg.value = stepRepository.getStepsAvg().value ?: 0
        }
    }
    fun updateStepsGoal(stepsGoal : Int) {
        viewModelScope.launch {
            _stepsGoal.value = stepsGoal ?: 0
        }
    }

    // Room Database에서 걸음수와 목표걸음수 가져와서 아이콘 색상을 계산하여 LiveData를 업데이트
    suspend fun updateCalendarIcons(calendar: CalendarView) {
        val stepsToday = stepRepository.getStepsToday().value ?: 0
        val stepsGoal = stepRepository.getStepsGoal().value ?: 0

        val iconColor = calculateIconColor(stepsToday, stepsGoal)

        val eventDay = EventDay(calendar, iconColor) // EventDay 생성

    }

    // 걸음수/목표걸음수의 비율에 따라 아이콘 색상을 계산
    private fun calculateIconColor(stepsToday: Int, stepsGoal: Int): Int {
        val ratio = stepsToday.toFloat() / stepsGoal.toFloat()

        return when {
            ratio >= 1.0 -> Color.BLUE // 파란색 아이콘
            ratio >= 0.5 -> Color.YELLOW // 노란색 아이콘
            else -> Color.RED // 빨간색 아이콘
        }
    }

}
