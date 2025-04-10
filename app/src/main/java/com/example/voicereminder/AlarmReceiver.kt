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
import androidx.core.content.ContextCompat.startForegroundService
import com.example.voicereminder.model.UpdateTriggerRequest
import com.example.voicereminder.network.ApiService
import com.example.voicereminder.network.RetrofitInstance.apiService
import com.example.voicereminder.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.sink
import java.io.File

import com.example.voicereminder.tts.AudioPlayerService
import okio.sink

class AlarmReceiver : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        // 1. 인텐트에서 데이터 추출 (setAlarm()에서 보낸 "MESSAGE" 사용)
        Log.d("AlarmReceiver", "AlarmReceiver AlarmReceiver:")
        // 알람 ID 추출 (필수!)
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", -1)
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

        //  알림 소리 및 진동 설정
        val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val vibratePattern = longArrayOf(1000, 1000, 1000, 1000)

        //  알림 빌더
        val builder = NotificationCompat.Builder(context, "ALARM_CHANNEL")
            .setSmallIcon(R.drawable.ic_notification)  // ⚠️ 리소스 ID 확인
            .setContentTitle("알람 (ID: $notificationId)")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(alarmSound)
            .setVibrate(vibratePattern)

        // 알림 표시
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        notificationManager.notify(notificationId, builder.build())
//        createUniqueId()

        //  TTS 음성 재생 (백그라운드에서 실행)
        CoroutineScope(Dispatchers.IO).launch {
            fetchAndPlayTTS(
                context = context,
                notificationId = notificationId
            )
        }

        // 2. 서버에 is_triggered 업데이트
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("Alarmupdate", "update is_triggered")
                val token = (context.applicationContext as MyApp).tokenManager.getAccessToken()
                val response = apiService.updateNotificationTrigger(
                    notificationId,
                    UpdateTriggerRequest(is_triggered = true),//알람이 울리고 트리거를 true로 바꿈으로써 똑같은 알람의 중복실행을 방지한다.
                    "Bearer $token"
                )
                if (response.isSuccessful) {
                    // ✅ 반드시 새로운 데이터 요청
                    Log.d("Alarmupdate successssssssss", "update is_triggered successsssss!!!!!!!!!!")
                    (context.applicationContext as MyApp)
                        .sentenceViewModel
                        .getSentence { error -> Log.e("Alarm", error) }
                }
                else if (!response.isSuccessful) {
                    Log.e("Alarmupdate", "서버 업데이트 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("Alarmupdate", "서버 통신 오류: ${e.message}")
            }
        }

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

    private suspend fun fetchAndPlayTTS(
        context: Context,
        notificationId: Int
    ) {
        try {

            val accessToken = TokenManager(context).getAccessToken()
            val response = apiService.getTTSByNotification(
                "Bearer $accessToken",
                NotificationID = notificationId
            )
            Log.d("fetchAndPlayTTS 함수 실행중", "respionse값: $response")
            if (response.isSuccessful) {
                response.body()?.use { body ->  // use()로 자원 자동 해제
                    val tempFile = File.createTempFile("tts", ".mp3", context.cacheDir).apply {
                        deleteOnExit()
                        body.source().use { source ->  // Okio의 source() 사용
                            outputStream().use { output ->
                                source.readAll(output.sink())  // Okio로 스트림 복사
                            }
                        }
                    }
                    Log.d("response 성공함 tts로 변경 성공", "tts tempfile: $tempFile")
                    startAudioService(context = context,file  = tempFile)
                }
            }

        } catch (e: Exception) {
            Log.e("FCM", "TTS 요청 실패", e)
        }
    }

    private fun startAudioService(context: Context,file: File) {
        Intent(context, AudioPlayerService::class.java).apply {
            putExtra("AUDIO_PATH", file.absolutePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d("startAudioService 성공", "tt")
                context.startForegroundService(this)
            } else {
                context.startService(this)
            }
        }
    }

        // 고유한 알림 ID 생성
    private fun createUniqueId(): Int = System.currentTimeMillis().toInt()
}