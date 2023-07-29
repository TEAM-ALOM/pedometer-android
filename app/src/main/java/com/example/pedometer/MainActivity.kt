    package com.example.pedometer

    import BaseActivity
    import SettingFragment
    import android.content.Context
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
    import androidx.health.connect.client.records.HeartRateRecord
    import androidx.health.connect.client.records.StepsRecord
    import androidx.health.connect.client.request.AggregateRequest
    import androidx.health.connect.client.time.TimeRangeFilter
    import androidx.lifecycle.lifecycleScope
    import com.example.pedometer.databinding.ActivityMainBinding
    import kotlinx.coroutines.launch
    import java.time.Instant


    class MainActivity : BaseActivity<ActivityMainBinding>({ ActivityMainBinding.inflate(it) }) {
        val textStepsToday by lazy { binding.viewStepsToday } // 현재 걸음 수
        val textStepsAvg by lazy { binding.viewStepsAvg } // 일주일간 평균 걸음 수
        lateinit var sharedPreferences: SharedPreferences



        override fun onCreate(savedInstanceState: Bundle?) {
            sharedPreferences = getSharedPreferences("stepsData", Context.MODE_PRIVATE)
            super.onCreate(savedInstanceState)
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
            textStepsToday.text = "현재 ${sharedPreferences.getInt("stepsToday",0)} 걸음"//현재 걸음 수
            textStepsAvg.text = "일주일간 평균 ${sharedPreferences.getInt("stepsAvg",0)} 걸음을 걸었습니다."//평균 걸음 수


            val providerPackageName="com.google.android.apps.healthdata"
            val availabilityStatus = HealthConnectClient.getSdkStatus(this, providerPackageName)
            if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
                return // early return as there is no viable integration
            }
            if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
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
            val healthConnectClient = HealthConnectClient.getOrCreate(this)
    // Issue operations with healthConnectClient
            updateStepsNow()
            updateStepsAverage()



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
        val PERMISSIONS =
            setOf(
                HealthPermission.getReadPermission(HeartRateRecord::class),
                HealthPermission.getWritePermission(HeartRateRecord::class),
                HealthPermission.getReadPermission(StepsRecord::class),
                HealthPermission.getWritePermission(StepsRecord::class)
            )
        val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

        val requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
            if (granted.containsAll(PERMISSIONS)) {
                // Permissions successfully granted
            } else {
                // Lack of required permissions
            }
        }

        suspend fun checkPermissionsAndRun(healthConnectClient: HealthConnectClient) {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (granted.containsAll(PERMISSIONS)) {
                // Permissions already granted; proceed with inserting or reading data
            } else {
                requestPermissions.launch(PERMISSIONS)
            }
        }
        override fun onResume() {
            super.onResume()
            // Health Connect SDK를 사용하여 주기적으로 stepCount 정보를 업데이트
            updateStepsNow()
            updateStepsAverage()

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
                    readStepsData(healthConnectClient)

                } else {
                    // 권한 요청
                    requestPermissions.launch(permissions)
                }
            }
        }
        private suspend fun readStepsData(healthConnectClient: HealthConnectClient) {
            // 현재 시간을 가져와서 endTime으로 설정
            val endTime = Instant.now()
            // 24시간 전의 시간을 가져와서 startTime으로 설정
            val startTime = endTime.minusSeconds(24 * 60 * 60)

            try {
                // 걸음 수 데이터 읽기
                val response = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                )
                // 데이터가 없는 경우에는 null이 반환될 수 있습니다
                val stepCount = response[StepsRecord.COUNT_TOTAL] as Long?
                // 업데이트된 stepsNow를 화면에 표시
                stepCount?.let {
                    sharedPreferences.edit().putInt("stepsToday",it.toInt()).apply()
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
                    readStepsDataAndCalculateAverage(healthConnectClient)

                } else {
                    // 권한 요청
                    requestPermissions.launch(permissions)
                }
            }
        }

        private suspend fun readStepsDataAndCalculateAverage(healthConnectClient: HealthConnectClient) {
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
                // 데이터가 없는 경우에는 null이 반환될 수 있습니다
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
