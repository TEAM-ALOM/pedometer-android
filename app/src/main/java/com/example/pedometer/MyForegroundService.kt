package com.example.pedometer

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.pedometer.Model.StepsDAO
import com.example.pedometer.Model.StepsDatabase
import com.example.pedometer.R.string.notification_channel_description
import com.example.pedometer.R.string.notification_channel_name
import com.example.pedometer.repository.StepRepository
import com.example.pedometer.repository.StepRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyForegroundService : LifecycleService() {

    private val notificationChannelID = "StepNotificationChannel"
    private val notificationID = 1
    private lateinit var stepRepository: StepRepository
    private lateinit var stepsDAO: StepsDAO
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        stepsDAO = StepsDatabase.getInstance(applicationContext).stepsDAO()
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        createNotificationChannel()
        sharedPreferences = getSharedPreferences("stepsData", MODE_PRIVATE)

        stepRepository = StepRepositoryImpl(this, stepsDAO)
        lifecycleScope.launch {
            val stepsToday = sharedPreferences.getInt("stepsToday",0)
            val stepsGoal = sharedPreferences.getInt("stepsGoal",0)
            val stepsPercent = if (stepsGoal > 0) ((stepsToday.toDouble() ?: 0.0) / stepsGoal.toDouble() * 100).toInt() else 0

            val notificationContent = getString(
                R.string.notification_content_text,
                stepsToday,
                stepsGoal,
                stepsPercent
            )

            val notificationIntent = Intent(this@MyForegroundService, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this@MyForegroundService, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

            val notification = buildNotification(notificationContent, pendingIntent)

            showNotification(notification)
        }
        return START_NOT_STICKY
    }


    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            notificationChannelID,
            getString(notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(notification_channel_description)
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(content: String, pendingIntent: PendingIntent): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, notificationChannelID)
            .setContentTitle(getString(R.string.notification_title))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun showNotification(notification: NotificationCompat.Builder) {
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            NotificationManagerCompat.from(this).notify(notificationID, notification.build())
        } else {
            val settingsIntent = Intent().apply {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("android.provider.extra.APP_PACKAGE", packageName)
            }
            val pendingIntent = PendingIntent.getActivity(this, 0, settingsIntent, 0)

            val notificationWithoutPermission = buildNotification(
                "알림 권한 설정 필요",
                pendingIntent
            )

            NotificationManagerCompat.from(this).notify(notificationID, notificationWithoutPermission.build())
        }
    }
}
