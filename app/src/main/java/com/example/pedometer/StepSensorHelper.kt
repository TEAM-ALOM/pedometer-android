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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StepSensorHelper(private val context: Context) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var _stepsToday = MutableLiveData<Int>()
    val stepsToday: LiveData<Int>
        get() = _stepsToday

    init {
        startListening()
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

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor == stepSensor) {
                val steps = it.values[0].toInt()
                _stepsToday.postValue(steps) // LiveData 값을 업데이트
                saveStepsToDatabase(steps)
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

}
