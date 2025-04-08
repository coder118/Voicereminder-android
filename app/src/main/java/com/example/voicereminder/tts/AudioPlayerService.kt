package com.example.voicereminder.tts

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import java.io.File

import android.app.NotificationChannel
import android.app.NotificationManager

import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.voicereminder.R
import androidx.media3.common.MediaItem

import android.net.Uri

class AudioPlayerService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "audio_player_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceWithNotification()
    }

    private fun createNotificationChannel() {
        Log.d("createNotificationChannel", "createNotificationChannel")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "음성 재생",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "음성 알림 재생 중"
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceWithNotification() {
        Log.d("startForegroundServiceWithNotification", "startForegroundServiceWithNotification")
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("음성 재생 중")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("AUDIO_PATH")?.let { path ->
            Log.d("AudioPlayerService 성공", "AudioPlayerService!!!!")
            playAudioFile(File(path))
        }
        return START_STICKY
    }

    private fun playAudioFile(file: File) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(file.absolutePath)
            prepareAsync()
            setOnPreparedListener { start() }
            setOnCompletionListener { stopSelf() }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null  // 바인딩을 지원하지 않음
    }
    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}