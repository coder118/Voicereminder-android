package com.example.voicereminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues.TAG
import android.content.Context
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val apiService: ApiService by lazy {
        RetrofitInstance.apiService // Retrofit 인스턴스를 가져오는 방법에 따라 수정
    }

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
        remoteMessage.notification?.let { notification ->
            val title = notification.title
            val body = notification.body

            // 알림 표시
            showNotification(title, body)

            // 앱 내 알림 업데이트를 위한 이벤트 발생
            EventBus.getDefault().post(NotificationEvent(title, body))
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


}
