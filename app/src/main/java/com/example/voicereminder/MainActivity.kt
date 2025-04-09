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
import com.example.voicereminder.tts.PlaybackService
import com.example.voicereminder.tts.TTSService
import kotlinx.coroutines.flow.combine
import java.text.ParseException
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
        // 시간 변경 관찰
//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                sentenceViewModel.savedTime.combine(sentenceViewModel.savedDate) { time, date ->
//                    Pair(time, date)
//                }.collect { (time, date) ->
//                    time?.let { t ->
//                        date?.let { d ->
//                            setAlarmFromDateTime(t, d)
//                        }
//                    }
//                }
//            }
//        }
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

//    private fun setAlarmFromDateTime(timeStr: String, dateStr: String) {
//        try {
//            // 날짜 파싱 (예: "2025-04-09" → Date 객체)
//            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
//
//            val date = dateFormat.parse(dateStr)
//            val time = timeFormat.parse(timeStr)
//
//            // 캘린더 조합
//            val calendar = Calendar.getInstance().apply {
//                time = date
//                set(Calendar.HOUR_OF_DAY, time.hours)
//                set(Calendar.MINUTE, time.minutes)
//                set(Calendar.SECOND, 0)
//            }
//
//            // 알람 설정
//            customAlarmManager.setAlarm(
//                calendar.timeInMillis,
//                "설정된 알람이 울립니다!"
//            )
//            Log.d("MainActivity", "Alarm set for: ${calendar.time}")
//
//        } catch (e: ParseException) {
//            Log.e("DateTimeError", "파싱 실패: ${e.message}")
//        }
//    }

    private fun setupAlarmFromNotification(notification: NotificationResponse) {
        try {
            // 날짜+시간 파싱 (예: "2025-04-10 14:30")
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



//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            VoiceReminderTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    VoiceReminderTheme {
//        Greeting("Android")
//    }
//}
//다른거입니다
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            VoiceReminderTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    AuthScreen()
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun AuthScreen() {
//    var isLogin by remember { mutableStateOf(false) }
//
//    if (isLogin) {
//        LoginScreen(onSignUpClick = { isLogin = false })
//    } else {
//        SignUpScreen(onLoginClick = { isLogin = true })
//    }
//}
//
//@Composable
//fun LoginScreen(onSignUpClick: () -> Unit) {
//    var username by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text(
//            text = "Login",
//            style = MaterialTheme.typography.headlineMedium
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//
//        OutlinedTextField(
//            value = username,
//            onValueChange = { username = it },
//            label = { Text("Username") },
//            modifier = Modifier.fillMaxWidth()
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//
//        OutlinedTextField(
//            value = password,
//            onValueChange = { password = it },
//            label = { Text("Password") },
//            visualTransformation = PasswordVisualTransformation(),
//            modifier = Modifier.fillMaxWidth()
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Button(
//            onClick = { /* TODO: Implement login logic */ },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text("Login")
//        }
//        Spacer(modifier = Modifier.height(8.dp))
//
//        TextButton(onClick = onSignUpClick) {
//            Text("Need an account? Sign Up")
//        }
//    }
//}
//
//@Composable
//fun SignUpScreen(onLoginClick: () -> Unit) {
//    var username by remember { mutableStateOf("") }
//    var email by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text(
//            text = "Sign Up",
//            style = MaterialTheme.typography.headlineMedium
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//
//        OutlinedTextField(
//            value = username,
//            onValueChange = { username = it },
//            label = { Text("Username") },
//            modifier = Modifier.fillMaxWidth()
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//
//        OutlinedTextField(
//            value = email,
//            onValueChange = { email = it },
//            label = { Text("Email") },
//            modifier = Modifier.fillMaxWidth()
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//
//        OutlinedTextField(
//            value = password,
//            onValueChange = { password = it },
//            label = { Text("Password") },
//            visualTransformation = PasswordVisualTransformation(),
//            modifier = Modifier.fillMaxWidth()
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Button(
//            onClick = { /* TODO: Implement sign up logic */ },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text("Sign Up")
//        }
//        Spacer(modifier = Modifier.height(8.dp))
//
//        TextButton(onClick = onLoginClick) {
//            Text("Already have an account? Login")
//        }
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun AuthScreenPreview() {
//    VoiceReminderTheme {
//        AuthScreen()
//    }
//}
//
//다른 것 테스트 성공한 코드
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            MyApp()
//        }
//    }
//
//    @Composable
//    fun MyApp() {
//        var tests by remember { mutableStateOf<List<Test>>(emptyList()) }
//
//        var newTestText by remember { mutableStateOf("") }  // 입력 필드
//
//        Column(modifier = Modifier.padding(16.dp)) {
//            Button(onClick = {
//                fetchTests { fetchedTests ->
//                    tests = fetchedTests ?: emptyList()
//                }
//            }) {
//                Text("Fetch Tests")
//            }
//
//            // 입력 필드
//            TextField(
//                value = newTestText,
//                onValueChange = { newTestText = it },
//                label = { Text("New Test") }
//            )
//
//            Button(onClick = {
//                val newTest = Test(id = 0, test = newTestText)  // Test 객체 생성
//                RetrofitInstance.apiService.createTest(newTest).enqueue(object : Callback<Test> {  // API 호출
//                    override fun onResponse(call: Call<Test>, response: Response<Test>) {
//                        if (response.isSuccessful) {
//                            Log.d("MainActivity", "Create Success: ${response.body()}")
//                            fetchTests { fetchedTests ->  // 목록 갱신
//                                tests = fetchedTests ?: emptyList()
//                            }
//                            newTestText = ""  // 입력 필드 초기화
//                        } else {
//                            Log.e("MainActivity", "Create Error: ${response.code()}")
//                        }
//                    }
//
//                    override fun onFailure(call: Call<Test>, t: Throwable) {
//                        Log.e("MainActivity", "Create Failure: ${t.message}")
//                    }
//                })
//            }) {
//                Text("Create Test")
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            tests.forEach { test ->
//                Text(text = "ID: ${test.id}, Test: ${test.test}")
//            }
//        }
//    }
//
//    private fun fetchTests(onResult: (List<Test>?) -> Unit) {
//        RetrofitInstance.apiService.getTests().enqueue(object : Callback<List<Test>> {
//            override fun onResponse(call: Call<List<Test>>, response: Response<List<Test>>) {
//                if (response.isSuccessful) {
//                    onResult(response.body())
//                } else {
//                    Log.e("MainActivity", "Error: ${response.code()}")
//                    onResult(null)
//                }
//            }
//
//            override fun onFailure(call: Call<List<Test>>, t: Throwable) {
//                Log.e("MainActivity", "Failure: ${t.message}")
//                onResult(null)
//            }
//        })
//    }
//
//
//}

