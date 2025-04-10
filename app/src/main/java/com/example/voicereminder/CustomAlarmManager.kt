package com.example.voicereminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Build
import android.util.Log

class CustomAlarmManager(private val context: Context) {
    private val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
    private val activeAlarms = mutableSetOf<Int>() // 활성 알람 ID 저장


    private fun isAlarmPending(notificationId: Int): Boolean {
        val intent = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null
    }

    fun setAlarm(notificationId: Int,timeInMillis: Long, message: String) {
        Log.d("CustomAlarmManager", "Setting alarm at: $timeInMillis")
//        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        if (isAlarmPending(notificationId)) {
//            Log.d("Alarm", "알람 중복 방지: ID $notificationId")
//            return
//        }
        // 시간 유효성 검사 추가
//        if (timeInMillis <= System.currentTimeMillis()) {
//            Log.e("Alarm", "과거 시간 알람 차단")
//            return
//        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("MESSAGE", message)
            putExtra("NOTIFICATION_ID", notificationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId, // 동일 ID로 취소 가능
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val clockInfo = AlarmManager.AlarmClockInfo(timeInMillis, pendingIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }


        // 성공 시 내부 리스트에 추가
        activeAlarms.add(notificationId)
        Log.d("Alarm", "알람 등록 성공: ID $notificationId")
    }

    fun cancelAlarm(notificationId: Int) {
        Log.d("CustomAlarmManager", "Cancelling alarm")
//        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        (context.getSystemService(ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
        //  실제 시스템 알람 취소
        alarmManager.cancel(pendingIntent)

        // 내부 리스트에서 제거
        activeAlarms.remove(notificationId)
        Log.d("Alarm", "알람 취소 성공: ID $notificationId")
    }
}