package com.example.pedometer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MyForegroundService : Service() {
    companion object {
        private const val notificationId = 1    // 알림 ID 정의
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Foreground에 서비스를 유지하기 위해 Notification 생성
        val notification = createNotification()

        // Foreground로 서비스 실행
        startForeground(notificationId, notification)

        // 여기서 주기적인 작업을 수행

        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = "myapp_notifications.foreground_service" // 알림 채널을 식별하고 구분하기 위해 사용됨.
        // 알림 생성을 위한 Builder 객체 생성
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("걸음 수 알림")
            .setContentText("오늘 걸음 수: {}, 목표 걸음 수: {}, 목표 달성률: {}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Android 8.0 (API 레벨 26) 이상부터는 채널을 설정해야 함.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}