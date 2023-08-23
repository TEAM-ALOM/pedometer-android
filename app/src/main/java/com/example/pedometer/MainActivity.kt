package com.example.pedometer
//
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.*
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.example.pedometer.Model.StepViewModel
import com.example.pedometer.Model.StepViewModelFactory
import com.example.pedometer.Model.StepsDatabase
import com.example.pedometer.Model.StepsEntity
import com.example.pedometer.databinding.ActivityMainBinding
import com.example.pedometer.fragment.Day
import com.example.pedometer.fragment.SettingFragment
import com.example.pedometer.repository.StepRepository
import com.example.pedometer.repository.StepRepositoryImpl
import com.github.mikephil.charting.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            val selectedMonth = clickedDate.get(Calendar.MONTH) + 1
            val selectedDay = clickedDate.get(Calendar.DAY_OF_MONTH)

            withContext(Dispatchers.IO) {
                val selectedDaySteps = getStepsForDate(clickedDate)
                val stepsGoal = sharedPreferences.getInt("stepsGoal", 0)

                withContext(Dispatchers.Main) {
                    showDayFragment(selectedDaySteps, stepsGoal, selectedMonth, selectedDay)
                    isDateClicked = true
                }
            }
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
    private fun showDayFragment(stepsCount: LiveData<StepsEntity>, stepsGoal: Int, selectedMonth: Int, selectedDay: Int) {
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


    private suspend fun getStepsForDate(date: Calendar): LiveData<StepsEntity> = withContext(Dispatchers.IO) {
        val stepsDAO = StepsDatabase.getInstance(this@MainActivity).stepsDAO() // StepsDAO 초기화

        val selectedDaySteps = stepsDAO.getByDate(date.timeInMillis) // Calendar를 Long으로 변환하여 전달

        MutableLiveData(selectedDaySteps)
    }









    @SuppressLint("CommitPrefEdits")
    private fun initializeViewModels() {
        // ViewModel 초기화 및 옵저빙 등 ViewModel 관련 작업 수행
        val stepsDAO = StepsDatabase.getInstance(this).stepsDAO()
        stepRepository = StepRepositoryImpl(this@MainActivity, stepsDAO) // stepsDAO 추가
        stepViewModelFactory = StepViewModelFactory(stepRepository)
        stepViewModel = ViewModelProvider(this, stepViewModelFactory)[StepViewModel::class.java]
        // LiveData 옵저빙 및 데이터 업데이트 작업 수행
        updateStepsData()
        val intent = Intent(this@MainActivity, MyForegroundService::class.java)
        stepViewModel.stepsToday.observe(this) { stepsToday ->
            binding.viewStepsToday.text = getString(R.string.steps_now,stepsToday)
            with(sharedPreferences.edit()) {
                putInt("stepsToday", stepsToday)
                apply()
            }
        }
        stepViewModel.stepsAvg.observe(this) { stepsAvg ->
            binding.viewStepsAvg.text = getString(R.string.steps_Avg,stepsAvg)
        }
        intent.putExtra("stepsToday", sharedPreferences.getInt("stepsToday",0))
        intent.putExtra("stepsGoal", sharedPreferences.getInt("stepsGoal",0))
        startService(intent)


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