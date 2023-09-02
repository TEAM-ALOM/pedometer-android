package com.example.pedometer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            val stepSensorHelper = StepSensorHelper(context, CoroutineScope(Dispatchers.IO))
            stepSensorHelper.onReboot()

            // 부팅 이후에 MyForegroundService를 호출
            val serviceIntent = Intent(context, MyForegroundService::class.java)
            context.startService(serviceIntent)
        }
    }
}



