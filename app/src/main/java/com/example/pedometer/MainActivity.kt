package com.example.pedometer
//
import BaseActivity
import Day
import SettingFragment
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.example.pedometer.databinding.ActivityMainBinding
import com.github.mikephil.charting.utils.Utils
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*


class MainActivity : BaseActivity<ActivityMainBinding>({ ActivityMainBinding.inflate(it) }){
    val textStepsToday by lazy { binding.viewStepsToday } // 현재 걸음 수
    val textStepsAvg by lazy { binding.viewStepsAvg } // 일주일간 평균 걸음 수
    lateinit var sharedPreferences: SharedPreferences
    private var isDateClicked = false // 클릭 여부를 저장하는 변수
    private var dayFragment: Day? = null // Day 프래그먼트를 저장하는 변수


    //lateinit var navController : NavController
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root//뷰 바인딩
        setContentView(view)
        setSupportActionBar(binding.toolbar.topAppBar3) // 수정된 코드: Toolbar를 바로 설정
        val moveToSettingIcon = binding.toolbar.topAppBar3.menu.findItem(R.id.moveToSettingIcon)
        moveToSettingIcon?.setOnMenuItemClickListener {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingFragment())
                .addToBackStack(null)
                .commit()
            true
        }
        sharedPreferences = getSharedPreferences("stepsData", MODE_PRIVATE)//걸음수 데이터 가져오기(앱 데이터)
        textStepsToday.text = "현재 ${sharedPreferences.getInt("stepsToday",0)} 걸음"//현재 걸음 수
        textStepsAvg.text = "일주일간 평균 ${sharedPreferences.getInt("stepsAvg",0)} 걸음을 걸었습니다."//평균 걸음 수

        val providerPackageName="com.google.android.apps.healthdata"//헬스커넥트 연결
        val availabilityStatus = HealthConnectClient.getSdkStatus(this, providerPackageName)
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            return
        }
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {//헬스 커넥터 미 설치 시 설치 경로 안내
            // Optionally redirect to package installer to find a provider, for example:
            val uriString = "market://details?id=$providerPackageName&url=https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata&hl=en-KR"
            this.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.android.vending")
                    data = Uri.parse(uriString)
                    putExtra("overlay", true)
                    putExtra("callerId", this@MainActivity.packageName)
                }
            )
            return
        }
        val healthConnectClient = HealthConnectClient.getOrCreate(this)//헬스 커넥트 연동 시작

        lifecycleScope.launch {//현재 걸음 수 업데이트
            updateStepsNow()
        }
        updateStepsAverage()//일주일간 평균 걸음 수 업데이트
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
    val PERMISSIONS =//권한 선언(걸음 수 읽기 쓰기)
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class)
        )
    val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()//권한 동의 생성

    val requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->//권한 요청 및 결과 처리
        if (granted.containsAll(PERMISSIONS)) {
            // Permissions successfully granted
        } else {
            // Lack of required permissions
        }
    }

    suspend fun checkPermissionsAndRun(healthConnectClient: HealthConnectClient) {//권한 확인 및 앱 열기
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        if (granted.containsAll(PERMISSIONS)) {
            // Permissions already granted; proceed with inserting or reading data
        } else {
            requestPermissions.launch(PERMISSIONS)
        }
    }
    override fun onResume() {
        super.onResume()
        // 이전 버튼 눌렀을 때와 날짜를 클릭하지 않았을 때 DayFragment를 숨김
        if (!isDateClicked) {
            hideDayFragment()
        }
        val calendarView: CalendarView = binding.calendarView
        calendarView.setOnDayClickListener(object : OnDayClickListener {
            override fun onDayClick(eventDay: EventDay) {
                onCalendarDayClicked(eventDay)
            }
        })
        // Health Connect SDK를 사용하여 주기적으로 stepCount 정보를 업데이트
        lifecycleScope.launch {
            updateStepsNow()
        }
        updateStepsAverage()


        // Material CalendarView 설정
        val calendarView = binding.calendarView

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
        // Health Connect Client 생성
        val healthConnectClient = HealthConnectClient.getOrCreate(this)
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


    private fun updateStepsNow() {
        // 걸음 수 데이터를 얻기 위해 읽기 권한 설정
        val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))

        // Health Connect Client 생성
        val healthConnectClient = HealthConnectClient.getOrCreate(this)

        // 허용된 권한 확인
        lifecycleScope.launch {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (granted.containsAll(permissions)) {
                // 걸음 수 데이터 읽기
                readStepsDataToday(healthConnectClient)

            } else {
                // 권한 요청
                requestPermissions.launch(permissions)
            }
        }
    }
    private suspend fun readStepsDataToday(healthConnectClient: HealthConnectClient) {
        // 현재 시간을 가져와서 endTime으로 설정
        val endTime = Instant.now().toEpochMilli()
        // 당일 00시를 startTime으로 설정
        val currentDayStart = Instant.now().truncatedTo(ChronoUnit.DAYS).toEpochMilli()

        try {
            // 걸음 수 데이터 읽기
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(
                        startTime = Instant.ofEpochMilli(currentDayStart),
                        endTime = Instant.ofEpochMilli(endTime)
                    )
                )
            )
            val stepCount = response[StepsRecord.COUNT_TOTAL] as Long?
            // 업데이트된 stepsNow를 화면에 표시
            stepCount?.let {
                sharedPreferences.edit().putInt("stepsToday", it.toInt()).apply()
                textStepsToday.text = "현재 ${sharedPreferences.getInt("stepsToday", 0)} 걸음"
            }
        } catch (e: Exception) {
            // 걸음 수 데이터 읽기 실패 시 에러 처리
            e.printStackTrace()
            // 또는 다른 방식으로 로그 출력
            Log.e("MainActivity", "걸음 수 데이터 읽기 실패: ${e.message}")
        }
    }





    private fun updateStepsAverage() {
        // 걸음 수 데이터를 얻기 위해 읽기 권한 설정
        val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))

        // Health Connect Client 생성
        val healthConnectClient = HealthConnectClient.getOrCreate(this)

        // 허용된 권한 확인
        lifecycleScope.launch {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (granted.containsAll(permissions)) {
                // 걸음 수 데이터 읽기
                readStepsDataAvg(healthConnectClient)

            } else {
                // 권한 요청
                requestPermissions.launch(permissions)
            }
        }
    }

    private suspend fun readStepsDataAvg(healthConnectClient: HealthConnectClient) {
        // 현재 시간을 가져와서 endTime으로 설정
        val endTime = Instant.now()
        // 1주일 전의 시간을 가져와서 startTime으로 설정
        val startTime = endTime.minusSeconds(7 * 24 * 60 * 60)

        try {
            // 걸음 수 데이터 읽기
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            val stepCount = response[StepsRecord.COUNT_TOTAL] as Long?

            // 일주일간의 평균 걸음수 계산
            val averageSteps = stepCount?.toFloat()?.div(7)?.toInt() ?: 0

            // 업데이트된 stepsNow와 stepsAvg를 화면에 표시
            stepCount?.let {
                sharedPreferences.edit().putInt("stepsAvg",it.toInt()).apply()
                textStepsAvg.text = "일주일간 평균 ${sharedPreferences.getInt("stepsAvg",0)} 걸음을 걸었습니다."
            }
        } catch (e: Exception) {
            // 걸음 수 데이터 읽기 실패 시 에러 처리
            e.printStackTrace()
            // 또는 다른 방식으로 로그 출력
            Log.e("MainActivity", "걸음 수 데이터 읽기 실패: ${e.message}")
        }

    }


}