package com.example.voicereminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp


import android.util.Log
import com.example.voicereminder.network.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.voicereminder.auth.AuthViewModel
import com.example.voicereminder.auth.LoginScreen
import com.example.voicereminder.auth.RegisterScreen
import com.example.voicereminder.main.MainScreen
import com.example.voicereminder.ui.theme.VoiceReminderTheme
import com.example.voicereminder.utils.TokenManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenManager = TokenManager(this)
        val authViewModel = AuthViewModel(
            RetrofitInstance.apiService,
            tokenManager
        )

        setContent {
            VoiceReminderTheme {
                val navController = rememberNavController()
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
                NavHost(
                    navController = navController,
                    startDestination = if (isLoggedIn) "main" else "login"
                ) {
                    composable("login") {
                        LoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = { authViewModel.setLoggedIn(true)
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

                            onLogoutSuccess = { authViewModel.setLoggedIn(false)
                                navController.navigate("login") {
                                    popUpTo("main") { inclusive = true } } },

                            onDeleteAccountSuccess = {
                                navController.navigate("login") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }


                        )

                    }
                }
            }
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

