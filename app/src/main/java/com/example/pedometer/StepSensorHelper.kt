package com.example.pedometer
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.pedometer.model.StepsDatabase
import com.example.pedometer.model.StepsEntity
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
@OptIn(DelicateCoroutinesApi::class)
class StepSensorHelper(private val context: Context) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var _stepsToday = MutableLiveData<Int>()
    private val stepsToday: LiveData<Int>
        get() = _stepsToday
    private var stepsPrev=0
    init {
        // 데이터베이스에서 전날 걸음수를 가져와 stepsPrev에 저장
        GlobalScope.launch(Dispatchers.IO) {
            stepsPrev = loadPreviousDaySteps()
        }
    }

    fun startListening() {
        stepSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        saveStepsToDatabase(stepsToday.value ?: 0) // 측정된 걸음수를 데이터베이스에 저장
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }
    fun stopListening() {
        sensorManager.unregisterListener(this)
    }
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor == stepSensor) {
                val steps = it.values[0].toInt()
                val realsteps=steps-stepsPrev
                _stepsToday.postValue(realsteps) // LiveData 값을 업데이트
                saveStepsToDatabase(realsteps)

            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveStepsToDatabase(steps: Int) {
        val currentTimeMillis = System.currentTimeMillis()
        val sharedPrefs = context.getSharedPreferences("stepsData", Context.MODE_PRIVATE)

        val stepsDAO = StepsDatabase.getInstance(context).stepsDAO()

        GlobalScope.launch(Dispatchers.IO) {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(currentTimeMillis))
            val existingEntity = stepsDAO.getByDate(currentDate)

            if (existingEntity != null) {
                // 이미 해당 날짜의 Entity가 존재하면 업데이트
                existingEntity.todaySteps = steps
                existingEntity.goalSteps = sharedPrefs.getInt("stepsGoal", 0)
                stepsDAO.update(existingEntity)
            } else {

                // 해당 날짜의 Entity가 없으면 새로 생성
                val stepsEntity = StepsEntity(
                    date = currentDate,
                    todaySteps = steps,
                    goalSteps = sharedPrefs.getInt("stepsGoal", 0)
                )
                stepsDAO.insert(stepsEntity)
            }
        }
    }
    // 데이터베이스에서 전날 걸음수를 가져와 저장
    private suspend fun loadPreviousDaySteps(): Int = withContext(Dispatchers.IO) {
        val currentTimeMillis = System.currentTimeMillis()
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(currentTimeMillis))

        val stepsDAO = StepsDatabase.getInstance(context).stepsDAO()
        val allStepsEntities = stepsDAO.getAll()

        // 오늘을 제외한 전체 날짜의 걸음수 합을 계산
        val totalSteps = allStepsEntities
            .filter { it.date != todayDate }
            .sumBy { it.todaySteps?:0 }

        totalSteps
    }
}
