import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pedometer.repository.StepRepository
import kotlinx.coroutines.launch

class StepViewModel(private val stepRepository: StepRepository) : ViewModel() {

    private val _stepsToday: MutableLiveData<Int> by lazy { MutableLiveData() }
    private val _stepsAvg: MutableLiveData<Int> by lazy { MutableLiveData() }

    val stepsToday: LiveData<Int>
        get() = _stepsToday
    val stepsAvg: LiveData<Int>
        get() = _stepsAvg

    fun updateStepsNow() {
        viewModelScope.launch {
            val stepsToday = stepRepository.getStepsToday().value ?: 0
            _stepsToday.value = stepsToday
            Log.e("StepViewModel", "오늘 걸음수: ${_stepsToday.value}")
            stepRepository.updateStepsNow()
        }
    }

    fun updateStepsAverage() {
        viewModelScope.launch {
            val stepsAvg = stepRepository.getStepsAvg().value ?: 0
            _stepsAvg.value = stepsAvg
            Log.e("StepViewModel", "일주일 평균 걸음수: ${_stepsAvg.value}")
            stepRepository.updateStepsAverage()
        }
    }

}
