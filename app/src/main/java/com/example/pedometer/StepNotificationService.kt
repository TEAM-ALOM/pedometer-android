package com.example.pedometer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class StepNotificationService : Service() {//걸음수 데이터 상태바에 알림

    private val NOTIFICATION_CHANNEL_ID = "StepNotificationChannel"
    private val NOTIFICATION_ID = 1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val stepsToday = intent?.getIntExtra("stepsToday", 0) ?: 0
        val stepsGoal = intent?.getIntExtra("stepsGoal", 0) ?: 0
        val stepsPercent = if (stepsGoal > 0) (stepsToday.toDouble() / stepsGoal.toDouble() * 100).toInt() else 0

        val notificationContent = getString(
            R.string.notification_content_text,
            stepsToday,
            stepsGoal,
            stepsPercent
        )

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification = buildNotification(notificationContent, pendingIntent)

        showNotification(notification)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_description)
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String, pendingIntent: PendingIntent): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setSmallIcon(R.drawable.check)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
    }

    private fun showNotification(notification: NotificationCompat.Builder) {
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification.build())
        } else {
            // Handle case where notifications are not enabled (optional)
            val settingsIntent = Intent().apply {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("android.provider.extra.APP_PACKAGE", packageName)
            }
            val pendingIntent = PendingIntent.getActivity(this, 0, settingsIntent, 0)

            val notificationWithoutPermission = buildNotification(
                "알림 권한 설정 필요",
                pendingIntent
            )

            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notificationWithoutPermission.build())
        }
    }
}
