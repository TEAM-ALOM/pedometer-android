
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

}
