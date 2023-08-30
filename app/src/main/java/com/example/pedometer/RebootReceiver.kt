package com.example.pedometer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            // 재부팅 후에 onReboot 메서드 호출
            GlobalScope.launch(Dispatchers.IO) {
                val stepSensorHelper = StepSensorHelper(context, CoroutineScope(Dispatchers.IO))
                stepSensorHelper.onReboot()
            }
        }
    }
}
