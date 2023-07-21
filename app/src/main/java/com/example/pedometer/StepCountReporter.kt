package com.example.pedometer

import android.os.Handler
import android.util.Log
import com.samsung.android.sdk.healthdata.HealthConstants.StepCount
import com.samsung.android.sdk.healthdata.HealthData
import com.samsung.android.sdk.healthdata.HealthDataObserver
import com.samsung.android.sdk.healthdata.HealthDataResolver
import com.samsung.android.sdk.healthdata.HealthDataResolver.AggregateRequest
import com.samsung.android.sdk.healthdata.HealthDataResolver.AggregateRequest.AggregateFunction
import com.samsung.android.sdk.healthdata.HealthDataResolver.AggregateResult
import com.samsung.android.sdk.healthdata.HealthDataStore
import java.util.*
import java.util.concurrent.TimeUnit

class StepCountReporter(
    private val mStore: HealthDataStore,
    private val mStepCountObserver: StepCountObserver,
    resultHandler: Handler?
) {
    private val mHealthDataResolver: HealthDataResolver
    private val mHealthDataObserver: HealthDataObserver

    init {
        mHealthDataResolver = HealthDataResolver(mStore, resultHandler)
        mHealthDataObserver = object : HealthDataObserver(resultHandler) {
            // Update the step count when a change event is received
            override fun onChange(dataTypeName: String) {
                Log.d("Pedometer", "Observer receives a data changed event")
                readTodayStepCount()
            }
        }
    }

    fun start() {
        // Register an observer to listen changes of step count and get today step count
        HealthDataObserver.addObserver(mStore, StepCount.HEALTH_DATA_TYPE, mHealthDataObserver)
        readTodayStepCount()
    }

    fun stop() {
        HealthDataObserver.removeObserver(mStore, mHealthDataObserver)
    }

    // Read the today's step count on demand
    private fun readTodayStepCount() {
        // Set time range from start time of today to the current time
        val startTime = getUtcStartOfDay(System.currentTimeMillis(), TimeZone.getDefault())
        val endTime = startTime + TimeUnit.DAYS.toMillis(1)
        val request = AggregateRequest.Builder()
            .setDataType(StepCount.HEALTH_DATA_TYPE)
            .addFunction(AggregateFunction.SUM, StepCount.COUNT, "total_step")
            .setLocalTimeRange(StepCount.START_TIME, StepCount.TIME_OFFSET, startTime, endTime)
            .build()
        try {
            mHealthDataResolver.aggregate(request)
                .setResultListener { aggregateResult: AggregateResult ->
                    aggregateResult.use { result ->
                        val iterator: Iterator<HealthData> = result.iterator()
                        if (iterator.hasNext()) {
                            mStepCountObserver.onChanged(iterator.next().getInt("total_step"))
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("Pedometer", "Getting step count fails.", e)
        }
    }

    private fun getUtcStartOfDay(time: Long, tz: TimeZone): Long {
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = time
        val year = cal[Calendar.YEAR]
        val month = cal[Calendar.MONTH]
        val date = cal[Calendar.DATE]
        cal.timeZone = TimeZone.getTimeZone("UTC")
        cal[Calendar.YEAR] = year
        cal[Calendar.MONTH] = month
        cal[Calendar.DATE] = date
        cal[Calendar.HOUR_OF_DAY] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        return cal.timeInMillis
    }

    interface StepCountObserver {
        fun onChanged(count: Int)
    }
}