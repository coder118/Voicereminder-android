package com.example.voicereminder.main


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.voicereminder.auth.AuthViewModel
import com.example.voicereminder.model.NotificationResponse
import kotlinx.coroutines.launch

import androidx.compose.foundation.lazy.items


//
//@Composable
//fun MainScreen(
//    viewModel: AuthViewModel,
//    onLogoutSuccess: () -> Unit, // 로그아웃 성공 시 호출될 콜백 함수
//    onDeleteAccountSuccess: () -> Unit,// 회우너 탈퇴성공시 콜백
//    onNavigateToCreateSentence: () -> Unit // 새로운 파라미터 추가
//) {
//    var errorMessage by remember { mutableStateOf<String?>(null) }
//    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
//
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(32.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text(
//            text = "메인 화면",
//            style = MaterialTheme.typography.headlineLarge,
//            modifier = Modifier.padding(bottom = 48.dp)
//        )
//
//        Button(
//            onClick = { onNavigateToCreateSentence() }, // 글쓰기 화면으로 이동
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text("글쓰기")
//        }
//
//        Button(
//            onClick = {
//                viewModel.logout(
//                    onSuccess = { onLogoutSuccess() } ,
//                    onError = { errorMessage = it }
//                )
//            },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text("로그아웃")
//        }
//
//        errorMessage?.let {
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                text = it,
//                color = MaterialTheme.colorScheme.error,
//                modifier = Modifier.padding(top = 8.dp)
//            )
//        }
//        //회원 탈퇴
//        Button(onClick = { showDeleteConfirmDialog = true }) {
//            Text("회원 탈퇴")
//        }
//
//        if (showDeleteConfirmDialog) {
//            AlertDialog(
//                onDismissRequest = { showDeleteConfirmDialog = false },
//                title = { Text("회원 탈퇴") },
//                text = { Text("정말로 회원 탈퇴하시겠습니까?") },
//                confirmButton = {
//                    Button(onClick = {
//                        viewModel.deleteAccount(
//                            onSuccess = {
//                                showDeleteConfirmDialog = false
//                                onDeleteAccountSuccess()
//                            },
//                            onError = { /* 에러 처리 */ }
//                        )
//                    }) {
//                        Text("확인")
//                    }
//                },
//                dismissButton = {
//                    Button(onClick = { showDeleteConfirmDialog = false }) {
//                        Text("취소")
//                    }
//                }
//            )
//        }
//
//    }
//}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AuthViewModel,
    //authViewModel: AuthViewModel,
    sentenceViewModel: SentenceViewModel,
    onLogoutSuccess: () -> Unit,
    onDeleteAccountSuccess: () -> Unit,
    onNavigateToCreateSentence: () -> Unit,
    //onNavigateToChangePassword: () -> Unit,
    // 문장 수정 화면으로 이동할 때, 클릭한 NotificationResponse를 넘겨주기
    onNavigateToEditSentence: (NotificationResponse) -> Unit
) {
    val scope = rememberCoroutineScope()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // 서버에서 받아온 알림(문장) 리스트
    val notificationList by sentenceViewModel.notificationList.collectAsState()

    // 화면 진입 시 서버 데이터 불러오기
    LaunchedEffect(Unit) {
        sentenceViewModel.getSentence(onError = { msg -> errorMessage = msg })
    }

    // 상단 우측 아이콘 클릭 시 열릴 DropdownMenu 제어
    var menuExpanded by remember { mutableStateOf(false) }

    // Scaffold로 전체 레이아웃 구성
    Scaffold(
        // 상단 AppBar
        topBar = {
            TopAppBar(
                title = { Text("메인 화면") },
                actions = {
                    // 상단 우측 동그란 아이콘 (회원 관련 메뉴)
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_myplaces),
                            contentDescription = "회원 메뉴"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        // 로그아웃
                        DropdownMenuItem(
                            text = { Text("로그아웃") },
                            onClick = {
                                menuExpanded = false
                                scope.launch {
                                    viewModel.logout(
                                        onSuccess = { onLogoutSuccess() },
                                        onError = { msg -> errorMessage = msg }
                                    )
                                }
                            }
                        )
//                        // 비밀번호 변경
//                        DropdownMenuItem(
//                            text = { Text("비밀번호 변경") },
//                            onClick = {
//                                menuExpanded = false
//                                onNavigateToChangePassword()
//                            }
//                        )
                        // 회원 탈퇴
                        DropdownMenuItem(
                            text = { Text("회원 탈퇴") },
                            onClick = {
                                menuExpanded = false
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            )
        },

        // 오른쪽 하단에 글쓰기 버튼
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToCreateSentence() }
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_input_add),
                    contentDescription = "글쓰기 버튼"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 알림(문장) 리스트
            if (notificationList.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notificationList) { item: NotificationResponse ->
                        NotificationItem(
                            item = item,
                            onClick = { clickedItem ->
                                // 문장 클릭 시 수정 화면으로 이동
                                onNavigateToEditSentence(clickedItem)
                            },
                            onDeleteClick = { clickedItem ->
                                // 문장 삭제
                                sentenceViewModel.deleteSentence(
                                    id = clickedItem.id,
                                    onSuccess = { /* 필요하면 스낵바나 메시지 */ },
                                    onError = { msg -> errorMessage = msg }
                                )
                            }
                        )
                    }
                }
            } else {
                // 목록이 비어있을 때
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("등록된 문장이 없습니다.")
                }
            }

            // 에러 메시지 표시
            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // 회원 탈퇴 확인 다이얼로그
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("회원 탈퇴") },
            text = { Text("정말로 회원 탈퇴하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(
                            onSuccess = {
                                showDeleteConfirmDialog = false
                                onDeleteAccountSuccess()
                            },
                            onError = {
                                errorMessage = it
                                showDeleteConfirmDialog = false
                            }
                        )
                    }
                ) {
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

@Composable
fun NotificationItem(
    item: NotificationResponse,
    onClick: (NotificationResponse) -> Unit,
    onDeleteClick: (NotificationResponse) -> Unit
) {
    // 한 행(Row) 전체를 클릭하면 수정 화면으로 이동, 오른쪽 버튼은 삭제
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable  { onClick(item) } // 아이템 전체 클릭 시 수정
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
//            Text(text = "내용: ${item.content ?: "없음"}")
//            Text(text = "날짜: ${item.date ?: "없음"} / 시간: ${item.time ?: "없음"}")
//            Text(text = "진동: ${if (item.vibrationEnabled) "ON" else "OFF"}, TTS ID: ${item.ttsVoiceId}")
//            Text(text = "랜덤알람: ${if (item.isRandom) "ON" else "OFF"}")
//
            Text(text = "내용: ${item.sentence.content ?: "없음"}")
            Text(text = "날짜: ${item.notificationSettings.notification_date ?: "없음"} / 시간: ${item.notificationSettings.notification_time ?: "없음"}")
            Text(text = "진동: ${if (item.userSettings.vibration_enabled) "ON" else "OFF"}, TTS ID: ${item.sentence.tts_voice}")
            Text(text = "랜덤알람: ${if (item.notificationSettings.repeat_mode == "random") "ON" else "OFF"}")

        }
        // 오른쪽에 삭제 버튼
        Button(onClick = { onDeleteClick(item) }) {
            Text("삭제")
        }
    }
}