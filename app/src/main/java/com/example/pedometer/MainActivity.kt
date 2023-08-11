package com.example.pedometer
//
import BaseActivity
import Day
import HealthPermissionTool
import SettingFragment
import StepViewModel
import StepViewModelFactory
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.example.pedometer.databinding.ActivityMainBinding
import com.example.pedometer.repository.StepRepository
import com.example.pedometer.repository.StepRepositoryImpl
import com.github.mikephil.charting.utils.Utils
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.*


class MainActivity : BaseActivity<ActivityMainBinding>({ ActivityMainBinding.inflate(it) }){

    lateinit var sharedPreferences: SharedPreferences
    private var isDateClicked = false
    private var dayFragment: Day? = null
    private lateinit var stepViewModelFactory: StepViewModelFactory
    private lateinit var stepViewModel: StepViewModel
    private lateinit var stepRepository: StepRepository
    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var healthPermissionTool: HealthPermissionTool

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("stepsData", MODE_PRIVATE)
        val view = binding.root
        setContentView(view)
        setSupportActionBar(binding.toolbar.topAppBar3)
        healthPermissionTool = HealthPermissionTool(this)
        lifecycleScope.launch {
            if (!healthPermissionTool.checkSdkStatusAndPromptForInstallation()) {
                // Health SDK가 사용 불가능하거나 설치가 필요한 경우 처리
                return@launch
            }

            healthConnectClient = HealthConnectClient.getOrCreate(this@MainActivity) // SDK 초기화

            initializeViewModels()
            initializeUI()

        }

        Utils.init(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.moveToSettingIcon -> {
                supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, SettingFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isDateClicked) {
            hideDayFragment()
        }
        lifecycleScope.launch {
            if (!healthPermissionTool.checkSdkStatusAndPromptForInstallation()) {
                return@launch
            }
            initializeViewModels()
            initializeUI()

        }
    }

    private fun onCalendarDayClicked(eventDay: EventDay) {
        lifecycleScope.launch {
            val clickedDate = eventDay.calendar
            val selectedDaySteps = getStepsForDate(clickedDate)
            val selectedMonth = clickedDate.get(Calendar.MONTH) + 1
            val selectedDay = clickedDate.get(Calendar.DAY_OF_MONTH)

            showDayFragment(
                selectedDaySteps,
                sharedPreferences.getInt("stepsGoal", 0),
                selectedMonth,
                selectedDay
            )
            isDateClicked = true
        }
    }
    override fun onBackPressed() {//이전 버튼이 눌렸을 때
        if (dayFragment != null && dayFragment!!.isVisible) {
            hideDayFragment()
            isDateClicked = false
        } else {
            super.onBackPressed()
        }
    }
    private fun showDayFragment(stepsCount: Int, stepsGoal: Int, selectedMonth: Int, selectedDay: Int) {
        dayFragment = Day(stepsCount, stepsGoal, selectedMonth, selectedDay)
        supportFragmentManager.beginTransaction()
            .add(android.R.id.content, dayFragment!!)
            .addToBackStack(null)
            .commit()
        isDateClicked = true
    }




    private fun hideDayFragment() {

        if (dayFragment != null) {
            supportFragmentManager.beginTransaction()
                .hide(dayFragment!!)
                .commit()
        }
        isDateClicked = false
    }


    private suspend fun getStepsForDate(date: Calendar): Int {

        val startTime = date.clone() as Calendar
        startTime.set(Calendar.HOUR_OF_DAY, 6)
        startTime.set(Calendar.MINUTE, 0)
        startTime.set(Calendar.SECOND, 0)
        startTime.set(Calendar.MILLISECOND, 0)

        val endTime = date.clone() as Calendar
        endTime.add(Calendar.DAY_OF_MONTH, 1) // 다음날로 이동
        endTime.set(Calendar.HOUR_OF_DAY, 5)
        endTime.set(Calendar.MINUTE, 59)
        endTime.set(Calendar.SECOND, 59)
        endTime.set(Calendar.MILLISECOND, 999)

        try {
            healthConnectClient = HealthConnectClient.getOrCreate(this)
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(
                        startTime = Instant.ofEpochMilli(startTime.timeInMillis),
                        endTime = Instant.ofEpochMilli(endTime.timeInMillis)
                    )
                )
            )

            val stepCount = response[StepsRecord.COUNT_TOTAL] as Long?
            return stepCount?.toInt() ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 0
    }

    private fun initializeViewModels() {
        // ViewModel 초기화 및 옵저빙 등 ViewModel 관련 작업 수행
        stepRepository = StepRepositoryImpl(this@MainActivity)
        stepViewModelFactory = StepViewModelFactory(stepRepository)
        stepViewModel = ViewModelProvider(this, stepViewModelFactory)
            .get(StepViewModel::class.java)
        // LiveData 옵저빙 및 데이터 업데이트 작업 수행
        updateStepsData()
        stepViewModel.stepsToday.observe(this, { stepsToday ->
            binding.viewStepsToday.text = "현재 $stepsToday 걸음"
            sharedPreferences.edit().putInt("stepsToday",stepsToday)
        })

        stepViewModel.stepsAvg.observe(this, { stepsAvg ->
            binding.viewStepsAvg.text = "일주일간 평균 $stepsAvg 걸음을 걸었습니다."
        })
    }


    private fun initializeUI() {
        // UI 초기화 작업 수행
        val moveToSettingIcon = binding.toolbar.topAppBar3.menu.findItem(R.id.moveToSettingIcon)
        moveToSettingIcon?.setOnMenuItemClickListener {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingFragment())
                .addToBackStack(null)
                .commit()
            true
        }

        val calendarView: CalendarView = binding.calendarView
        calendarView.setOnDayClickListener(object : OnDayClickListener {
            override fun onDayClick(eventDay: EventDay) {
                onCalendarDayClicked(eventDay)
            }
        })
        val intent = Intent(this@MainActivity, StepNotificationService::class.java)
        intent.putExtra("stepsToday", sharedPreferences.getInt("stepsToday",0))
        intent.putExtra("stepsGoal", sharedPreferences.getInt("stepsGoal",0))
        startService(intent)
    }

    private fun updateStepsData() {
        // 걸음 수 데이터 업데이트 작업 수행
        stepViewModel.updateStepsNow()
        stepViewModel.updateStepsAverage()
    }



}