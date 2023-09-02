package com.example.pedometer

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.MutableLiveData
import com.example.pedometer.model.StepsDatabase
import com.example.pedometer.model.StepsEntity
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
@OptIn(DelicateCoroutinesApi::class)
class StepSensorHelper(private val context: Context, private val scope: CoroutineScope) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var _stepsToday = MutableLiveData<Int>()
    private var hasRebooted = false // 재부팅 여부를 나타내는 플래그 추가
    private var bootDate: Long = 0 // 핸드폰 부팅한 날짜를 저장할 변수 추가
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("pedometerPrefs", Context.MODE_PRIVATE)

    private var stepsPrev = 0
    init {
        // 데이터베이스에서 전날 걸음수를 가져와 stepsPrev에 저장
        scope.launch(Dispatchers.IO) {
            // 부팅 날짜 가져오기
            bootDate = sharedPreferences.getLong("bootDate", 0)

            // 리부팅 날짜가 저장되어 있지 않으면 센서값에서 전날들의 걸음수 총합을 빼줘야 함
            if (bootDate == 0L) {
                stepsPrev = loadTotalSteps()
            } else {
                // 리부팅 날짜부터 어제까지의 걸음수 계산
                val currentTimeMillis = System.currentTimeMillis()
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(currentTimeMillis))
                val yesterdayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(currentTimeMillis - 24 * 60 * 60 * 1000))
                stepsPrev = getStepsBetweenDates(yesterdayDate, currentDate)

            }
        }
    }

    fun startListening() {
        stepSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor == stepSensor) {
                val steps = it.values[0].toInt()
                val realsteps = steps-stepsPrev
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
                hasRebooted = false//새로운 엔티티가 생긴 후 저장되었으므로 리부팅 메세지 철회
                stepsDAO.insert(stepsEntity)
            }
        }
    }

    // 데이터베이스에서 전날 걸음수를 가져와 저장
    private suspend fun loadTotalSteps(): Int = withContext(Dispatchers.IO) {
        val currentTimeMillis = System.currentTimeMillis()
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(currentTimeMillis))

        val stepsDAO = StepsDatabase.getInstance(context).stepsDAO()
        val allStepsEntities = stepsDAO.getAll()
        val todaySteps=stepsDAO.getByDate(todayDate)?.todaySteps?:0

        // 전체 날짜의 걸음수 합을 계산
        val totalSteps = allStepsEntities
            .sumBy { it.todaySteps ?: 0 } - todaySteps

        totalSteps

    }

    // 지정된 날짜 범위 내의 걸음수를 가져옴
    private suspend fun getStepsBetweenDates(startTime: String, endTime: String): Int = withContext(Dispatchers.IO) {
        if (startTime > endTime) {//초기화를 한 날짜가 어제 이후인 경우/ 즉 오늘 초기화가 되었다면 stepsPrev에 0 저장하여 차감 x
            return@withContext 0
        }
        val stepsDAO = StepsDatabase.getInstance(context).stepsDAO()
        val stepsEntities = stepsDAO.getStepsBetweenDates(startTime, endTime)

        // 주어진 날짜 범위 내의 걸음수 합을 계산
        val totalSteps = stepsEntities.sumBy { it.todaySteps ?: 0 }

        totalSteps
    }

    fun onReboot() {
        hasRebooted = true
        bootDate = System.currentTimeMillis() // 핸드폰 부팅 날짜 업데이트
        sharedPreferences.edit().putLong("bootDate", bootDate).apply()
    }
}
