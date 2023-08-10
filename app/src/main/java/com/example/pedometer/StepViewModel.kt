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
            _stepsToday.value = stepRepository.getStepsToday().value ?: 0
            Log.e("StepViewModel", "오늘 걸음수: ${_stepsToday.value}")
        }
    }

    fun updateStepsAverage() {
        viewModelScope.launch {
            _stepsAvg.value = stepRepository.getStepsAvg().value ?: 0
            Log.e("StepViewModel", "일주일 평균 걸음수: ${_stepsAvg.value}")
        }
    }

}
