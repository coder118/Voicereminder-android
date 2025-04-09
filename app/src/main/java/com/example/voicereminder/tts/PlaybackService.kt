package com.example.voicereminder.tts
//
//import com.example.voicereminder.tts.MediaNotificationHelper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import android.util.Log
import com.example.voicereminder.R
import androidx.core.app.NotificationCompat

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var ttsRealTime: TTSRealTime? = null

    override fun onCreate() {
        super.onCreate()

        // ExoPlayer 생성
        val player = ExoPlayer.Builder(this).build()

        // MediaSession 생성 및 연결
        mediaSession = MediaSession.Builder(this, player).build()

        // Foreground Notification 설정 (필수)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "MEDIA_CHANNEL_ID",
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        // MediaNotificationHelper를 사용하여 알림 생성
//        val notification = MediaNotificationHelper.createNotification(
//            this,
//            mediaSession!!,
//            R.drawable.ic_notification, // 알림 아이콘
//            "Media Playback",           // 알림 제목
//            "Playing audio..."          // 알림 내용
//        )
//
//        startForeground(notification.notificationId, notification.notification)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = intent?.getStringExtra("USER_ID") // 사용자 ID 가져오기
        if (userId != null) {
            Log.d("PlaybackService", "Received USER_ID: $userId")

//            // TTSRealTime 초기화 및 WebSocket 연결 설정
//            val player = mediaSession?.player ?: return START_NOT_STICKY
//            ttsRealTime = TTSRealTime(userId, this, player)
//            ttsRealTime?.connect()
        } else {
            Log.e("PlaybackService", "USER_ID is null.")
        }

//        return super.onStartCommand(intent, flags, startId) // 반드시 호출해야 함
        return START_STICKY // 서비스가 강제 종료되면 자동으로 재시작
    }

    // MediaController가 연결할 수 있는 MediaSession 반환
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        super.onDestroy()

        // ExoPlayer 및 MediaSession 해제
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }

        // TTSRealTime 연결 종료
        ttsRealTime?.disconnect()
    }
}