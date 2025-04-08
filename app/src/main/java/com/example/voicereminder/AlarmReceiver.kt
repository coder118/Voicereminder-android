package com.example.voicereminder

import android.app.NotificationChannel
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 1. 인텐트에서 데이터 추출 (setAlarm()에서 보낸 "MESSAGE" 사용)
        Log.d("AlarmReceiver", "AlarmReceiver AlarmReceiver:")
        val message = intent.getStringExtra("MESSAGE") ?: "알람이 울립니다!"

        // 2. 알림 채널 생성 (Android 8.0+ 필수)
        createNotificationChannel(context)

        //  알람 실행 시 이동할 화면 설정 (예: "AlarmFragment"로 직접 이동)
        val navIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAV_DESTINATION", "alarm_screen") // 네비게이션 대상 지정
            putExtra("ALARM_DATA", intent.getStringExtra("MESSAGE")) // 알람 데이터 전달
        }

        //  PendingIntent 설정
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(), // 고유 ID 생성
            navIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 4. 알림 소리 및 진동 설정
        val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val vibratePattern = longArrayOf(1000, 1000, 1000, 1000)

        // 5. 알림 빌더
        val builder = NotificationCompat.Builder(context, "ALARM_CHANNEL")
            .setSmallIcon(R.drawable.ic_notification)  // ⚠️ 리소스 ID 확인
            .setContentTitle("알람")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(alarmSound)
            .setVibrate(vibratePattern)

        // 6. 알림 표시
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        notificationManager.notify(createUniqueId(), builder.build())
    }

    // 알림 채널 생성 (Android 8.0+)
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ALARM_CHANNEL",  // setAlarm()과 동일한 채널 ID
                "알람 채널",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "알람 알림 채널"
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000)
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // 고유한 알림 ID 생성
    private fun createUniqueId(): Int = System.currentTimeMillis().toInt()
}