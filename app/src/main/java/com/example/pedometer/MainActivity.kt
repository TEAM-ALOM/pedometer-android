package com.example.pedometer

import BaseActivity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.example.pedometer.databinding.ActivityMainBinding
import com.example.pedometer.fragment.Day
import kotlinx.coroutines.launch
import java.time.Instant


class MainActivity : BaseActivity<ActivityMainBinding>({ ActivityMainBinding.inflate(it) }) {
    val textStepsToday by lazy { binding.viewStepsToday } // 현재 걸음 수
    val textStepsAvg by lazy { binding.viewStepsAvg } // 일주일간 평균 걸음 수
    object GlobalVariables {
        var stepsNow: Int = 0
        var stepsGoal: Int = 8000
        var stepsAvg: Int = 0

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = binding.root//뷰 바인딩
        setContentView(view)

        textStepsToday.text = "현재 ${GlobalVariables.stepsNow} 걸음"//현재 걸음 수
        textStepsAvg.text = "일주일간 평균 ${GlobalVariables.stepsAvg} 걸음을 걸었습니다."//평균 걸음 수
        supportFragmentManager.beginTransaction()// com.example.pedometer.fragment.Day 프래그먼트 frame layout에 전시
            .add(R.id.frameLayout, Day())
            .commit()

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
                GlobalVariables.stepsNow = it.toInt()
                textStepsToday.text = "현재 ${GlobalVariables.stepsNow} 걸음"

                val dayFragment = supportFragmentManager.findFragmentById(R.id.frameLayout)
                if (dayFragment is Day) {
                    dayFragment.updatePieChart(GlobalVariables.stepsNow, GlobalVariables.stepsGoal)
                }
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
                GlobalVariables.stepsAvg = averageSteps
                textStepsAvg.text = "일주일간 평균 ${GlobalVariables.stepsAvg} 걸음을 걸었습니다."

                val dayFragment = supportFragmentManager.findFragmentById(R.id.frameLayout)
                if (dayFragment is Day) {
                    dayFragment.updatePieChart(GlobalVariables.stepsNow, GlobalVariables.stepsGoal)
                }
            }
        } catch (e: Exception) {
            // 걸음 수 데이터 읽기 실패 시 에러 처리
            e.printStackTrace()
            // 또는 다른 방식으로 로그 출력
            Log.e("MainActivity", "걸음 수 데이터 읽기 실패: ${e.message}")
        }
    }
}
