package com.example.voicereminder.main


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.voicereminder.auth.AuthViewModel

@Composable
fun MainScreen(
    viewModel: AuthViewModel,
    onLogoutSuccess: () -> Unit, // 로그아웃 성공 시 호출될 콜백 함수
    onDeleteAccountSuccess: () -> Unit// 회우너 탈퇴성공시 콜백
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "메인 화면",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Button(
            onClick = {
                viewModel.logout(
                    onSuccess = { onLogoutSuccess() } ,
                    onError = { errorMessage = it }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("로그아웃")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        //회원 탈퇴
        Button(onClick = { showDeleteConfirmDialog = true }) {
            Text("회원 탈퇴")
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("회원 탈퇴") },
                text = { Text("정말로 회원 탈퇴하시겠습니까?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deleteAccount(
                            onSuccess = {
                                showDeleteConfirmDialog = false
                                onDeleteAccountSuccess()
                            },
                            onError = { /* 에러 처리 */ }
                        )
                    }) {
                        Text("확인")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDeleteConfirmDialog = false }) {
                        Text("취소")
                    }
                }
            )
        }

    }
}
