package com.example.pedometer

import BaseActivity
import Day
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.example.pedometer.StepCountReporter.StepCountObserver
import com.example.pedometer.databinding.ActivityMainBinding
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult
import com.samsung.android.sdk.healthdata.HealthConstants.StepCount
import com.samsung.android.sdk.healthdata.HealthDataStore
import com.samsung.android.sdk.healthdata.HealthDataStore.ConnectionListener
import com.samsung.android.sdk.healthdata.HealthPermissionManager
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionKey
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionType


class MainActivity : BaseActivity<ActivityMainBinding>({ ActivityMainBinding.inflate(it) }) {
    val textStepsToday by lazy { binding.viewStepsToday } // 현재 걸음 수
    val textStepsAvg by lazy { binding.viewStepsAvg } // 일주일간 평균 걸음 수
    object GlobalVariables {
        var stepsNow: Int = 0
        var stepsGoal: Int = 0
        var stepsAvg: Int = 0

    }
    var stepsNow=GlobalVariables.stepsNow
    var stepsGoal=GlobalVariables.stepsGoal
    var stepsAvg=GlobalVariables.stepsAvg
    val APP_TAG = "Pedometer"
    private lateinit var mStore: HealthDataStore
    private lateinit var mReporter: StepCountReporter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = binding.root//뷰 바인딩
        setContentView(view)
        // Create a HealthDataStore instance and set its listener
        mStore = HealthDataStore(this, mConnectionListener)
        // Request the connection to the health data store
        // Request the connection to the health data store
        mReporter = StepCountReporter(mStore!!, mStepCountObserver, Handler(Looper.getMainLooper()))
        mStore.connectService()
        mStepCountObserver = object : StepCountObserver {
            override fun onChanged(count: Int) {
                Log.d(APP_TAG, "Step reported : $count")
                updateStepCountView(count.toString())
            }
        }
        textStepsToday.setTextSize(14f)
        textStepsToday.setTextSize(28f)

        textStepsToday.text = "현재 $stepsNow 걸음"//현재 걸음 수
        textStepsAvg.text = "일주일간 평균 $stepsAvg 걸음을 걸었습니다."//평균 걸음 수
        supportFragmentManager.beginTransaction()// Day 프래그먼트 frame layout에 전시
            .add(R.id.frameLayout, Day())
            .commit()

    }
    override fun onDestroy() {
        mReporter!!.stop()
        mStore!!.disconnectService()
        super.onDestroy()
    }

    private val mConnectionListener: ConnectionListener = object : ConnectionListener {
        override fun onConnected() {
            Log.d(APP_TAG, "Health data service is connected.")
            mReporter =
                StepCountReporter(mStore!!, mStepCountObserver, Handler(Looper.getMainLooper()))
            if (isPermissionAcquired()) {
                mReporter.start()
            } else {
                requestPermission()
            }
        }

        override fun onConnectionFailed(error: HealthConnectionErrorResult) {
            Log.d(APP_TAG, "Health data service is not available.")
            showConnectionFailureDialog(error)
        }

        override fun onDisconnected() {
            Log.d(APP_TAG, "Health data service is disconnected.")
            if (!isFinishing) {
                mStore!!.connectService()
            }
        }
    }

    private fun showPermissionAlarmDialog() {
        if (isFinishing) {
            return
        }
        val alert = AlertDialog.Builder(this@MainActivity)
        alert.setTitle(R.string.notice)
            .setMessage(R.string.msg_perm_acquired)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showConnectionFailureDialog(error: HealthConnectionErrorResult) {
        if (isFinishing) {
            return
        }
        val alert = AlertDialog.Builder(this)
        if (error.hasResolution()) {
            when (error.errorCode) {
                HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED -> alert.setMessage(R.string.msg_req_install)
                HealthConnectionErrorResult.OLD_VERSION_PLATFORM -> alert.setMessage(R.string.msg_req_upgrade)
                HealthConnectionErrorResult.PLATFORM_DISABLED -> alert.setMessage(R.string.msg_req_enable)
                HealthConnectionErrorResult.USER_AGREEMENT_NEEDED -> alert.setMessage(R.string.msg_req_agree)
                else -> alert.setMessage(R.string.msg_req_available)
            }
        } else {
            alert.setMessage(R.string.msg_conn_not_available)
        }
        alert.setPositiveButton(R.string.ok) { dialog, id ->
            if (error.hasResolution()) {
                error.resolve(this@MainActivity)
            }
        }
        if (error.hasResolution()) {
            alert.setNegativeButton(R.string.cancel, null)
        }
        alert.show()
    }

    private fun isPermissionAcquired(): Boolean {
        val permKey = PermissionKey(StepCount.HEALTH_DATA_TYPE, PermissionType.READ)
        val pmsManager = HealthPermissionManager(mStore)
        try {
            // Check whether the permissions that this application needs are acquired
            val resultMap = pmsManager.isPermissionAcquired(setOf(permKey))
            return !resultMap.containsValue(java.lang.Boolean.FALSE)
        } catch (e: Exception) {
            Log.e(APP_TAG, "Permission request fails.", e)
        }
        return false
    }

    private fun requestPermission() {
        val permKey = PermissionKey(StepCount.HEALTH_DATA_TYPE, PermissionType.READ)
        val pmsManager = HealthPermissionManager(mStore)
        try {
            // Show user permission UI for allowing user to change options
            pmsManager.requestPermissions(setOf<PermissionKey>(permKey), this@MainActivity)
                .setResultListener { result: HealthPermissionManager.PermissionResult ->
                    Log.d(APP_TAG, "Permission callback is received.")
                    val resultMap =
                        result.resultMap
                    if (resultMap.containsValue(java.lang.Boolean.FALSE)) {
                        updateStepCountView("")
                        showPermissionAlarmDialog()
                    } else {
                        // Get the current step count and display it
                        mReporter!!.start()
                    }
                }
        } catch (e: Exception) {
            Log.e(APP_TAG, "Permission setting fails.", e)
        }
    }

    private var mStepCountObserver: StepCountObserver = object : StepCountObserver {
        override fun onChanged(count: Int) {
            Log.d(APP_TAG, "Step reported : $count")
            updateStepCountView(count.toString())
        }
    }

    private fun updateStepCountView(count: String) {
        runOnUiThread {
            stepsNow= count.toFloat().toInt()
            textStepsToday.text = "현재 $stepsNow 걸음"//현재 걸음 수

        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.connect) {
            requestPermission()
        }
        return true
    }

}
