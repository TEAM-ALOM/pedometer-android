package com.example.pedometer

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.*
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.example.pedometer.databinding.ActivityMainBinding
import com.example.pedometer.fragment.Day
import com.example.pedometer.fragment.SettingFragment
import com.example.pedometer.model.StepViewModel
import com.example.pedometer.model.StepViewModelFactory
import com.example.pedometer.model.StepsDatabase
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
    private var isForeground = false
    private lateinit var sharedPreferences: SharedPreferences
    private var isDateClicked = false
    private var dayFragment: Day? = null
    private lateinit var stepViewModelFactory: StepViewModelFactory
    private lateinit var stepViewModel: StepViewModel
    private lateinit var stepRepository: StepRepository
    private lateinit var calendarView: CalendarView
    private var backPressedTime: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("stepsData", MODE_PRIVATE)
        val view = binding.root
        setContentView(view)
        setSupportActionBar(binding.toolbar.topAppBar3)
        Utils.init(this)
        lifecycleScope.launch {
            observeViewModel()
            initializeUI()
        }

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
        isForeground = true
        if (!isDateClicked) {
            hideDayFragment()
        }
        if (isForeground && !isInBackground()) { // Check if app is in the foreground
            updateUI()
        }
    }
    override fun onPause() {
        super.onPause()
        isForeground = false
    }
    private fun isInBackground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses
        for (appProcess in appProcesses) {
            if (appProcess.processName == packageName) {
                return appProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            }
        }
        return false
    }

    private fun onCalendarDayClicked(eventDay: EventDay) {
        lifecycleScope.launch {
            val clickedDate = eventDay.calendar
            val selectedMonth = clickedDate.get(Calendar.MONTH) + 1
            val selectedDay = clickedDate.get(Calendar.DAY_OF_MONTH)
            val selectedDate = clickedDate.timeInMillis

            withContext(Dispatchers.IO) {
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = dateFormatter.format(Date(selectedDate))

                val stepsEntity = StepsDatabase.getInstance(this@MainActivity).stepsDAO().getByDate(currentDate)
                val stepsGoal = stepsEntity?.goalSteps ?: 0
                val selectedDaySteps = stepsEntity?.todaySteps ?: 0
                withContext(Dispatchers.Main) {
                    showDayFragment(selectedDaySteps, stepsGoal, selectedMonth, selectedDay)
                    isDateClicked = true
                }
            }
        }
    }

    private fun showDayFragment(stepsCount: Int, stepsGoal: Int, selectedMonth: Int, selectedDay: Int) {
        dayFragment = Day(stepsCount, stepsGoal, selectedMonth, selectedDay)
        supportFragmentManager.beginTransaction()
            .add(android.R.id.content, dayFragment!!)
            .commit()
        isDateClicked = true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (dayFragment != null && dayFragment!!.isVisible) {
            hideDayFragment()
            isDateClicked = false
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressedTime < 2000) {
                super.onBackPressed()
            } else {
                backPressedTime = currentTime
                Toast.makeText(this, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideDayFragment() {
        if (dayFragment != null && dayFragment!!.isVisible) {
            supportFragmentManager.beginTransaction()
                .hide(dayFragment!!)
                .commit()
        }
    }

    @SuppressLint("CommitPrefEdits")
    private fun observeViewModel() = lifecycleScope.launch {
        // ViewModel 초기화 및 옵저빙 등 ViewModel 관련 작업 수행
        val stepsDAO = StepsDatabase.getInstance(this@MainActivity).stepsDAO()

        stepRepository = StepRepositoryImpl(stepsDAO) // stepsDAO 추가
        stepViewModelFactory = StepViewModelFactory(stepRepository)

        stepViewModel = ViewModelProvider(this@MainActivity, stepViewModelFactory)[StepViewModel::class.java]

        // LiveData 옵저빙 및 데이터 업데이트 작업 수행
        updateStepsData()

        stepViewModel.stepsToday.observe(this@MainActivity) { stepsToday ->
            lifecycleScope.launch {
                stepViewModel.updateStepsNow()
            }
            binding.viewStepsToday.text = getString(R.string.steps_now, stepsToday)
        }
        stepViewModel.stepsAvg.observe(this@MainActivity) { stepsAvg ->
            lifecycleScope.launch {
                stepViewModel.updateStepsAverage()

            }
            binding.viewStepsAvg.text = getString(R.string.steps_Avg, stepsAvg)
        }
    }

    private fun initializeUI() {
        // UI 초기화 작업 수행
        calendarView = binding.calendarView
        val moveToSettingIcon = binding.toolbar.topAppBar3.menu.findItem(R.id.moveToSettingIcon)
        moveToSettingIcon?.setOnMenuItemClickListener {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingFragment())
                .addToBackStack(null)
                .commit()
            true
        }
        calendarView.setOnDayClickListener(object : OnDayClickListener {
            override fun onDayClick(eventDay: EventDay) {
                onCalendarDayClicked(eventDay)
            }
        })
    }
    private fun updateUI(){
        calendarView= binding.calendarView // 달력
        lifecycleScope.launch {
            stepViewModel.updateCalendarIcons(calendarView)
        }
        val intent = Intent(this, MyForegroundService::class.java)
        startService(intent)
    }

    private suspend fun updateStepsData() {
        // 걸음 수 데이터 업데이트 작업 수행
        stepViewModel.updateStepsNow()
        stepViewModel.updateStepsAverage()
    }


}
