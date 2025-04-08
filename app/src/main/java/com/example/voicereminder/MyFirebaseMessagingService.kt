package com.example.voicereminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.voicereminder.auth.AuthViewModel
import com.example.voicereminder.network.RetrofitInstance
import com.example.voicereminder.utils.TokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.greenrobot.eventbus.EventBus
import com.example.voicereminder.model.NotificationEvent
import com.example.voicereminder.network.ApiService
import com.example.voicereminder.tts.TTSHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.net.Uri
import com.example.voicereminder.tts.AudioPlayerService
import okio.sink
import java.io.File

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val apiService: ApiService by lazy {
        RetrofitInstance.apiService // Retrofit 인스턴스를 가져오는 방법에 따라 수정
    }
    private var mediaPlayer: MediaPlayer? = null


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // 새 토큰이 생성될 때마다 서버로 전송
        Log.d("onNEWTOKEN", "ONNEWTOKEN working? Refreshed token: $token")
//        val authViewModel = AuthViewModel(RetrofitInstance.apiService, TokenManager(this))
//        authViewModel.updateFcmToken(token)
        val user_accesstoken_for_fcm = TokenManager(this).getAccessToken()
        if (user_accesstoken_for_fcm != null) {
            // FCM 토큰을 서버로 전송
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("onNEWTOKEN", "ONNEWTOKEN working222222222? Refreshed token: $token")
                sendTokenToServer(user_accesstoken_for_fcm, token)
            }
        } else {
            Log.e(TAG, "Access token을 찾을 수 없습니다.")
        }

    }

    suspend fun sendTokenToServer(token: String, user_accesstoken_for_fcm: String) {
        // Django 서버에 토큰을 저장하는 API 호출
        val response = apiService.updateFcmToken("Bearer $user_accesstoken_for_fcm", mapOf("fcm_token" to token)) // Retrofit 또는 다른 네트워크 라이브러리 사용
        if (response.isSuccessful) {
            Log.d("sendTokenToserver working!!", "Token saved successfully for user: $user_accesstoken_for_fcm")
        } else {
            Log.e(TAG, "Failed to save token for user: $user_accesstoken_for_fcm")
        }
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // FCM 메시지 수신 처리

        // 데이터 부분 처리
        remoteMessage.data.let { data ->
            val sentenceId = data["sentence_id"]?.toLongOrNull()
            val userId = data["user_id"]?.toLongOrNull()

            Log.d("onMessageReceived 데이터부분", "sentence 아이디값: $sentenceId")
            if (sentenceId != null && userId != null) {
                // TTS 음성 요청 및 재생 로직 실행

                CoroutineScope(Dispatchers.IO).launch {
                    Log.d("fetchand play tts", "make tts ? userid n:$userId")
                    fetchAndPlayTTS(sentenceId, userId)
                }
                // 앱 내 알림 업데이트
                EventBus.getDefault().post(NotificationEvent(
                    title = data["title"],
                    content = data["body"]
                ))
            }
        }

        // 알림 표시 부분
        remoteMessage.notification?.let { notification ->
            val title = notification.title
            val body = notification.body
            showNotification(title, body)
        }
    }

    private fun showNotification(title: String?, body: String?) {
        // 알림 채널 생성 (Android 8.0 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default_channel_id",
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 생성 및 표시
        val notificationBuilder = NotificationCompat.Builder(this, "default_channel_id")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)// 클릭 시 알림 삭제
        // 알림 표시
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }


    private suspend fun fetchAndPlayTTS(sentenceId: Long, userId: Long) {//fcm으로 실시간으로 알람을 받아오고 문장을 다시 서버로 전송해서 tts로 반환받아온다.

            try {

                val accessToken = TokenManager(this).getAccessToken()
                val response = apiService.getTTSByNotification(
                    "Bearer $accessToken",
                    sentenceId = sentenceId,
                    userId = userId
                )
                Log.d("fetchAndPlayTTS 함수 실행중", "respionse값: $response")
                if (response.isSuccessful) {
                    response.body()?.use { body ->  // use()로 자원 자동 해제
                        val tempFile = File.createTempFile("tts", ".mp3", cacheDir).apply {
                            deleteOnExit()
                            body.source().use { source ->  // Okio의 source() 사용
                                outputStream().use { output ->
                                    source.readAll(output.sink())  // Okio로 스트림 복사
                                }
                            }
                        }
                        Log.d("response 성공함 tts로 변경 성공", "tts tempfile: $tempFile")
                        startAudioService(tempFile)
                    }
                }

            } catch (e: Exception) {
                Log.e("FCM", "TTS 요청 실패", e)
            }

    }

    private fun startAudioService(file: File) {
        Intent(applicationContext, AudioPlayerService::class.java).apply {
            putExtra("AUDIO_PATH", file.absolutePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d("startAudioService 성공", "tt")
                startForegroundService(this)
            } else {
                startService(this)
            }
        }
    }

//    private fun playTTS(text: String) {
//        CoroutineScope(Dispatchers.IO).launch {
//            TTSHelper.synthesizeText(applicationContext, text)?.let { file ->
//                Log.d("playtts","tts wokring")
//                playAudioFile(applicationContext, file)
//            }
//        }
//    }
//
//    private fun playAudioFile(appContext: Context, file: File) {
//        try {
//            Log.d("playaudiofile","play wokring")
//            // 기존 재생 중인 음성 정리
//            releaseMediaPlayer()
//
//            // 새 MediaPlayer 인스턴스 생성
//            mediaPlayer = MediaPlayer().apply {
//                setDataSource(appContext, Uri.fromFile(file))
//                prepareAsync() // 비동기 준비
//
//                setOnPreparedListener { mp ->
//                    mp.start() // 준비 완료 시 재생 시작
//                }
//
//                setOnCompletionListener {
//                    releaseMediaPlayer()
//                    file.delete() // 재생 완료 후 임시 파일 삭제
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            releaseMediaPlayer()
//        }
//    }
//
//    private fun releaseMediaPlayer() {
//        mediaPlayer?.let {
//            if (it.isPlaying) {
//                it.stop()
//            }
//            it.release()
//        }
//        mediaPlayer = null
//    }

}
