import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pedometer.repository.StepRepository

class StepViewModelFactory(private val stepRepository: StepRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StepViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StepViewModel(stepRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
