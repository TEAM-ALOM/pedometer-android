package com.example.pedometer
//
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.example.pedometer.Model.StepViewModel
import com.example.pedometer.Model.StepViewModelFactory
import com.example.pedometer.Model.StepsDAO
import com.example.pedometer.Model.StepsDatabase
import com.example.pedometer.databinding.ActivityMainBinding
import com.example.pedometer.fragment.Day
import com.example.pedometer.fragment.SettingFragment
import com.example.pedometer.repository.StepRepository
import com.example.pedometer.repository.StepRepositoryImpl
import com.github.mikephil.charting.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*


@Suppress("DEPRECATION")
class MainActivity : BaseActivity<ActivityMainBinding>({ ActivityMainBinding.inflate(it) }){
    private lateinit var sharedPreferences: SharedPreferences
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
            val selectedDaySteps = withContext(Dispatchers.IO) {
                stepRepository.getByDate(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(clickedDate.time)
                )?.todaySteps?:0
            }
            val selectedGoalSteps = withContext(Dispatchers.IO) {
                stepRepository.getByDate(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(clickedDate.time)
                )?.goalSteps?:0
            }
            val selectedMonth = clickedDate.get(Calendar.MONTH) + 1
            val selectedDay = clickedDate.get(Calendar.DAY_OF_MONTH)

            showDayFragment(
                selectedDaySteps,
                selectedGoalSteps,
                selectedMonth,
                selectedDay
            )
            isDateClicked = true
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = //이전 버튼이 눌렸을 때
        if (dayFragment != null && dayFragment!!.isVisible) {
            hideDayFragment()
            isDateClicked = false
        } else {
            super.onBackPressed()
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


    private fun getStepsForDate(date: Calendar): Int {
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.time)
        val stepsDAO = StepsDatabase.getInstance(this)?.stepsDAO()
        val stepsEntity = stepsDAO?.getByDate(formattedDate)

        return stepsEntity?.todaySteps ?: 0
    }

    @SuppressLint("CommitPrefEdits")
    private fun initializeViewModels() {
        // ViewModel 초기화 및 옵저빙 등 ViewModel 관련 작업 수행
        val stepsDAO = StepsDatabase.getInstance(this@MainActivity)?.stepsDAO() // StepsDAO를 가져옴
        val nonNullableStepsDAO: StepsDAO = stepsDAO ?: error("StepsDAO must not be null")
        stepRepository = StepRepositoryImpl(nonNullableStepsDAO, this@MainActivity)
        stepViewModelFactory = StepViewModelFactory(stepRepository)
        stepViewModel = ViewModelProvider(this, stepViewModelFactory)[StepViewModel::class.java]
        // LiveData 옵저빙 및 데이터 업데이트 작업 수행
        updateStepsData()

        val intent = Intent(this@MainActivity, MyForegroundService::class.java)
        intent.putExtra("stepsGoal", sharedPreferences.getInt("stepsGoal",0))
        stepViewModel.stepsToday.observe(this) { stepsToday ->
            binding.viewStepsToday.text = getString(R.string.steps_now,stepsToday)
            intent.putExtra("stepsToday", stepsToday)
            sharedPreferences.edit().putInt("stepsToday", stepsToday)
            startService(intent)
        }
        stepViewModel.stepsGoal.observe(this) { stepsGoal ->
            intent.putExtra("stepsGoal", stepsGoal)
            sharedPreferences.edit().putInt("stepsGoal", stepsGoal)
            startService(intent)
        }
        stepViewModel.stepsAvg.observe(this) { stepsAvg ->
            binding.viewStepsAvg.text = getString(R.string.steps_Avg,stepsAvg)
        }


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


        val calendarView: CalendarView = binding.calendarView//달력
        calendarView.setOnDayClickListener(object : OnDayClickListener {
            override fun onDayClick(eventDay: EventDay) {
                onCalendarDayClicked(eventDay)
            }
        })


    }

    private fun updateStepsData() {
        // 걸음 수 데이터 업데이트 작업 수행
        stepViewModel.updateStepsNow()
        stepViewModel.updateStepsAverage()
    }

}