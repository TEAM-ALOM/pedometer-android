package com.example.pedometer
//
import BaseActivity
import Day
import HealthPermissionTool
import SettingFragment
import StepViewModel
import StepViewModelFactory
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
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
import java.time.temporal.ChronoUnit
import java.util.*


class MainActivity : BaseActivity<ActivityMainBinding>({ ActivityMainBinding.inflate(it) }){
    val textStepsToday by lazy { binding.viewStepsToday } // 현재 걸음 수/
    val textStepsAvg by lazy { binding.viewStepsAvg } // 일주일간 평균 걸음 수
    lateinit var sharedPreferences: SharedPreferences
    private var isDateClicked = false // 클릭 여부를 저장하는 변수
    private var dayFragment: Day? = null // Day 프래그먼트를 저장하는 변수
    private lateinit var stepViewModelFactory: StepViewModelFactory
    private lateinit var stepViewModel: StepViewModel
    private lateinit var stepRepository: StepRepository
    private lateinit var healthConnectClient: HealthConnectClient // healthConnectClient를 클래스 레벨에서 선언
    private lateinit var healthPermissionTool: HealthPermissionTool




    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("stepsData", MODE_PRIVATE)//걸음수 데이터 가져오기(앱 데이터)

        val view = binding.root//뷰 바인딩
        setContentView(view)
        setSupportActionBar(binding.toolbar.topAppBar3) // 수정된 코드: Toolbar를 바로 설정
        healthPermissionTool = HealthPermissionTool(this)
        healthConnectClient = HealthConnectClient.getOrCreate(this)

        stepRepository = StepRepositoryImpl(sharedPreferences, healthConnectClient) // healthConnectClient 초기화 이후에 stepRepository 초기화



        lifecycleScope.launch {
            if (!healthPermissionTool.checkSdkStatusAndPromptForInstallation()) {
                return@launch
            }


            updateStepsData()
            initializeViewModels()
            initializeUI()
        }

        Utils.init(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {//바 메뉴 생성
        menuInflater.inflate(R.menu.top_app_bar, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {// 탑바 설정 아이콘 클릭 되었을 때
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
        // 이전 버튼 눌렀을 때와 날짜를 클릭하지 않았을 때 DayFragment를 숨김
        if (!isDateClicked) {
            hideDayFragment()
        }
        lifecycleScope.launch {
            if (!healthPermissionTool.checkSdkStatusAndPromptForInstallation()) {
                return@launch
            }
            updateStepsData()

            initializeViewModels()

            initializeUI()
            Log.e("MainActivity", "업데이트 성공")

        }


    }
    private fun onCalendarDayClicked(eventDay: EventDay) {
        lifecycleScope.launch {
            val clickedDate = eventDay.calendar
            val selectedDaySteps = getStepsForDate(clickedDate)
            val selectedMonth = clickedDate.get(Calendar.MONTH) + 1
            val selectedDay = clickedDate.get(Calendar.DAY_OF_MONTH)


            if (selectedDaySteps != null) {
                showDayFragment(
                    selectedDaySteps,
                    sharedPreferences.getInt("stepsGoal", 0),
                    selectedMonth,
                    selectedDay
                )

            } else {
                // 처리할 작업이 없는 경우는 여기에 추가
            }

            // 날짜가 클릭되었으므로 isDateClicked 변수를 true로 설정합니다.
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
    private fun showDayFragment(stepsCount: Int,stepsGoal:Int,selectedMonth: Int, selectedDay: Int) {
        if (dayFragment == null) {
            dayFragment = Day()
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, dayFragment!!)
                .addToBackStack(null)
                .commit()
        } else {
            supportFragmentManager.beginTransaction()
                .show(dayFragment!!)
                .commit()
        }
        // 걸음 수 데이터를 Day 프래그먼트로 전달합니다.
        dayFragment?.setStepsData(stepsCount,stepsGoal, selectedMonth, selectedDay)
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


    private suspend fun getStepsForDate(date: Calendar): Int {//걸음수 데이터 가져오기(달력 날짜 선택용)

        // 당일 00시를 startTime으로 설정
        val startTime = Instant.ofEpochMilli(date.timeInMillis).truncatedTo(ChronoUnit.DAYS)
        // 다음날 00시를 endTime으로 설정 (excluded)
        val endTime = Instant.ofEpochMilli(date.timeInMillis).plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)

        try {
            // 걸음 수 데이터 읽기
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(
                        startTime = startTime,
                        endTime = endTime
                    )
                )
            )

            val stepCount = response[StepsRecord.COUNT_TOTAL] as Long?

            return stepCount?.toInt() ?: 0
        } catch (e: Exception) {
            // 걸음 수 데이터 읽기 실패 시 에러 처리
            e.printStackTrace()
            // 또는 다른 방식으로 로그 출력
            Log.e("MainActivity", "걸음 수 데이터 읽기 실패: ${e.message}")
        }

        return 0
    }
    private fun initializeViewModels() {
        // ViewModel 초기화 및 옵저빙 등 ViewModel 관련 작업 수행
        stepViewModelFactory = StepViewModelFactory(stepRepository)
        stepViewModel = ViewModelProvider(this, stepViewModelFactory)
            .get(StepViewModel::class.java)

        // LiveData 옵저빙 및 데이터 업데이트 작업 수행
        stepViewModel.stepsToday.observe(this, { stepsToday ->
            binding.viewStepsToday.text = "현재 $stepsToday 걸음"
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
    }

    private suspend fun updateStepsData() {
        // 걸음 수 데이터 업데이트 작업 수행
        stepRepository.updateStepsNow()
        stepRepository.updateStepsAverage()
    }



}