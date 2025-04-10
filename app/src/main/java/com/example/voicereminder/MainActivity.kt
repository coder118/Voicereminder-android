package com.example.voicereminder


import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.voicereminder.auth.AuthViewModel
import com.example.voicereminder.auth.LoginScreen
import com.example.voicereminder.auth.RegisterScreen
import com.example.voicereminder.main.CreateSentenceScreen
import com.example.voicereminder.main.EditSentenceScreen
import com.example.voicereminder.main.MainScreen
import com.example.voicereminder.main.SentenceViewModel
import com.example.voicereminder.network.RetrofitInstance
import com.example.voicereminder.ui.theme.VoiceReminderTheme
import com.example.voicereminder.utils.TokenManager
import com.example.voicereminder.tts.AudioPlayerService

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.voicereminder.model.NotificationResponse
import com.example.voicereminder.tts.TTSService
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar

class MainActivity : ComponentActivity() {

    public lateinit var customAlarmManager: CustomAlarmManager //알람 매니저를 가져온다.

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val userId ="abc"// ViewModel에서 사용자 ID 가져오기
        val intent = Intent(this@MainActivity, AudioPlayerService::class.java).apply {
            putExtra("USER_ID", userId.toString()) // 사용자 ID 전달
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("MainActivity11", "Starting Foreground Service")
            startForegroundService(intent) // Foreground Service 시작
        } else {
            Log.d("22222", "Starting Background Service")
            startService(intent) // 일반 서비스 시작 (Android O 미만)
        }


        val tokenManager = TokenManager(this)
        val authViewModel = AuthViewModel(//로그인,회원가입,로그아웃
            RetrofitInstance.apiService,
            tokenManager
        )
        val sentenceViewModel = SentenceViewModel(RetrofitInstance.apiService, tokenManager) //글작성


        customAlarmManager = CustomAlarmManager(this)//알람매니저 초기화

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sentenceViewModel.notificationList.collect { notifications ->
                    notifications.forEach { notification ->
                            setupAlarmFromNotification(notification)
                    }
                }
            }
        }


        setContent {
            VoiceReminderTheme {
                val navController = rememberNavController()
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

                // 알람 인텐트 처리 LaunchedEffect
                LaunchedEffect(Unit) {
                    Log.d("move to createsetense","move to createsentence")
                    val navDestination = intent?.getStringExtra("NAV_DESTINATION")
                    val alarmMessage = intent?.getStringExtra("ALARM_DATA")

                    if (navDestination == "create_sentence") {
                        navController.navigate("createSentence") {
                            popUpTo("main") { inclusive = false }
                        }
                        // 필요 시 sentenceViewModel에 알람 데이터 전달
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (isLoggedIn) "main" else "login"
                ) {
                    composable("login") {
                        LoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = {
//                                val userId = authViewModel.getUserId() // ViewModel에서 사용자 ID 가져오기
//                                val intent = Intent(this@MainActivity, TTSService::class.java).apply {
//                                    putExtra("USER_ID", userId.toString()) // 사용자 ID 전달
//                                }
//                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                                    Log.d("MainActivity11", "Starting Foreground Service")
//                                    startForegroundService(intent) // Foreground Service 시작
//                                } else {
//                                    Log.d("22222", "Starting Background Service")
//                                    startService(intent) // 일반 서비스 시작 (Android O 미만)
//                                }


                                authViewModel.setLoggedIn(true)
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                } },
                            onNavigateToRegister = { navController.navigate("register") }
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            viewModel = authViewModel,
                            onRegisterSuccess = {
                                // 회원가입 성공 시 로그인 화면으로 이동
                                navController.navigate("login") {
                                    popUpTo("register") { inclusive = true }
                                }
                            },
                            onNavigateToLogin = { navController.popBackStack() }
                        )
                    }
                    composable("main") {
                        // 메인 화면 구현 부분
                        MainScreen(
                            viewModel = authViewModel,
                            sentenceViewModel = sentenceViewModel,

                            onLogoutSuccess = {
                                // WebSocket 연결 종료
                                val intent = Intent(this@MainActivity, TTSService::class.java)
                                stopService(intent) // TTSService 종료

                                authViewModel.setLoggedIn(false)
                                navController.navigate("login") {
                                    popUpTo("main") { inclusive = true } } },

                            onDeleteAccountSuccess = {
                                navController.navigate("login") {
                                    popUpTo("main") { inclusive = true }
                                }
                            },


                            onNavigateToCreateSentence = {
                                navController.navigate("createSentence")
                            },

                            onNavigateToEditSentence = { item ->
                                navController.navigate("editScreen/${item.id}")
                            },

                            onNavigateToTTS = { navController.navigate("tts") }


                        )

                    }

                    composable("createSentence") {
                        CreateSentenceScreen(
                            sentenceViewModel = sentenceViewModel,
                            onCancel = { navController.popBackStack() },
                            onSubmitSuccess = {
                                navController.popBackStack()
                                // 여기에 성공 메시지를 표시하는 로직을 추가할 수 있습니다.
                            }
                        )
                    }

                    composable(
                        "editScreen/{sentenceId}",
                        arguments = listOf(navArgument("sentenceId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val sentenceId = backStackEntry.arguments?.getInt("sentenceId") ?: -1
                        val originalItem = sentenceViewModel.getSentenceById(sentenceId)
                        originalItem?.let {
                            EditSentenceScreen(
                                sentenceViewModel = sentenceViewModel,
                                originalItem = it,
                                onCancel = { navController.popBackStack() },
                                onUpdateSuccess = {
                                    lifecycleScope.launch {
                                        delay(1500)// 1.5초 대기
                                    navController.navigate("main") {
                                        popUpTo("main") { inclusive = true }
                                    }
                                    }
                                }
                            )
                        } ?: run {
                            // 원본 아이템을 찾지 못했을 때의 처리
                            Text("문장을 찾을 수 없습니다.")
                        }
                    }

                }
            }
        }
    }


    private fun setupAlarmFromNotification(notification: NotificationResponse) {
        try {
            // 날짜+시간 파싱 (예: "2025-04-10 14:30")
            Log.d("checking trigger","check$notification")
            // checkNotificationResponse(id=62, sentence=Sent....., is_triggered=false)
            if (notification.is_triggered) {
                Log.d("Alarm", "이미 실행된 알람 무시: ID ${notification.id}")
                return
            }
            val dateTimeStr = "${notification.notificationSettings.notification_date} " +
                    "${notification.notificationSettings.notification_time}"
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val alarmTime = sdf.parse(dateTimeStr)?.time ?: return
            Log.d("setupAlarmFromNotification","setupAlarmFromNotification$alarmTime")
            // 알람 설정
            customAlarmManager.setAlarm(
                notificationId = notification.id, // 서버에서 받은 고유 ID
                timeInMillis = alarmTime,
                message = notification.sentence.content ?: "알람이 울립니다"
            )

        } catch (e: Exception) {
            Log.e("AlarmSetup", "알람 설정 실패: ${e.message}")
        }
    }
}



