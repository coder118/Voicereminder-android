package com.example.voicereminder.auth

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.voicereminder.model.User
import com.google.firebase.messaging.FirebaseMessaging
import androidx.compose.ui.platform.LocalContext

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()

    var fcmToken by remember { mutableStateOf<String?>(null) }

    var showPopup by remember { mutableStateOf(false) }
    var popupMessage by remember { mutableStateOf("") }


    LaunchedEffect(authState) {
        when (authState) {
            is AuthViewModel.AuthState.Success -> onLoginSuccess()
            is AuthViewModel.AuthState.Idle -> {} // Idle 상태에서는 아무 작업도 하지 않음 로그 아웃할때 사용됨

            is AuthViewModel.AuthState.ShowPopup -> {//같은 아이디로 여러기기에 로그인을 하려할때 띄우는 팝업
                popupMessage = (authState as AuthViewModel.AuthState.ShowPopup).message
                showPopup = true
            }
            else -> {}
        }
    }
    if (showPopup) {
        ShowPopup(message = popupMessage) {
            showPopup = false // 팝업 닫기
        }
    }
//    LaunchedEffect(Unit) {//로그인 화면에서 바로 fcmtoken생성
//        try {
//            FirebaseMessaging.getInstance().token
//                .addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        fcmToken = task.result
//                        Log.d("FCM", "토큰 생성 성공: $fcmToken")
//                    } else {
//                        Log.e("FCM", "토큰 생성 실패", task.exception)
//                    }
//                }
//        } catch (e: Exception) {
//            Log.e("FCM", "FCM 토큰 가져오기 중 예외 발생", e)
//        }
//    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Voice Reminder",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("사용자 이름") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (authState) {
            is AuthViewModel.AuthState.Error -> {
                Text(
                    text = (authState as AuthViewModel.AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            else -> {}
        }

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    viewModel.setError("모든 필드를 입력해주세요")
                } else {
                    viewModel.login(User(username, password,fcm_token = null))
//                    fcmToken?.let { token ->
//                        viewModel.login(User(username, password, fcm_token = token))
//                    } ?: viewModel.setError("FCM 토큰을 가져오는데 실패했습니다.")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("로그인")
        }

        TextButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("회원가입 하러 가기")
        }
    }
}

@Composable//팝업띄우는 기능
fun ShowPopup(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("알림") },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = { onDismiss() }) {
                Text("확인")
            }
        }
    )
}
