package com.example.pedometer.model
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pedometer.repository.StepRepository

class StepViewModelFactory(private val stepRepository: StepRepository) : ViewModelProvider.Factory {//걸음수 뷰모델 팩토리
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StepViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StepViewModel(stepRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
