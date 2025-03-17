package com.example.voicereminder.main

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.voicereminder.main.SentenceViewModel
import com.example.voicereminder.model.NotificationResponse
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSentenceScreen(
    sentenceViewModel: SentenceViewModel,
    originalItem: NotificationResponse, // 수정할 문장(알림) 데이터를 통째로 넘김
    onCancel: () -> Unit,
    onUpdateSuccess: () -> Unit
) {
    // 기존 데이터를 초기값으로 설정
    var content by remember { mutableStateOf(originalItem.sentence.content ?: "") }
    var selectedTime by remember { mutableStateOf(parseTime(originalItem.notificationSettings.notification_time)) }
    var selectedDate by remember { mutableStateOf(parseDate(originalItem.notificationSettings.notification_date)) }
    // isRandom을 Boolean으로 설정
    var isRandom by remember { mutableStateOf(originalItem.notificationSettings.repeat_mode == "random") }
    var vibrationEnabled by remember { mutableStateOf(originalItem.userSettings.vibration_enabled) }
    var ttsVoiceId by remember { mutableStateOf(originalItem.userSettings.tts_voice) }

    val sentenceState by sentenceViewModel.sentenceState.collectAsState()

    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime?.hour ?: 12,
        initialMinute = selectedTime?.minute ?: 0
    )
    val datePickerState = rememberDatePickerState()

    // timePickerState 변경 시 selectedTime 업데이트
    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
    }

    // datePickerState.selectedDateMillis가 null이 아닐 때 selectedDate 업데이트
    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("문장을 입력하세요...") },
                    label = { Text("내용") }
                )
            }
            item {
                if (!isRandom) {
                    // 시간 선택
                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // 날짜 선택
                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isRandom,
                        onCheckedChange = { isRandom = it }
                    )
                    Text("랜덤 알람 설정")
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it }
                    )
                    Text("진동 활성화")
                }
            }
            item {
                TextField(
                    value = ttsVoiceId.toString(),
                    onValueChange = { ttsVoiceId = it.toIntOrNull() ?: 0 },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("TTS 음성 ID를 입력하세요") },
                    label = { Text("TTS 음성 ID") }
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = onCancel) {
                        Text("취소")
                    }
                    Button(onClick = {
                        // ViewModel의 updateSentence 호출
                        sentenceViewModel.updateSentence(
                            id = originalItem.id,
                            content = content,
                            time = selectedTime?.toString(),
                            date = selectedDate?.toString(),
                            isRandom = isRandom,
                            ttsVoiceId = ttsVoiceId,
                            vibrationEnabled = vibrationEnabled,
                            onSuccess = { onUpdateSuccess() },
                            onError = { /* 에러 처리 */ }
                        )
                    }) {
                        Text("수정 완료")
                    }
                }
            }
        }

        when (sentenceState) {
            is SentenceViewModel.SentenceState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            is SentenceViewModel.SentenceState.Success -> {
                Text("문장이 성공적으로 수정되었습니다!", color = MaterialTheme.colorScheme.primary)
            }
            is SentenceViewModel.SentenceState.Error -> {
                val errorMessage = (sentenceState as SentenceViewModel.SentenceState.Error).message
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
            else -> {}
        }
    }
}

/**
 * 문자열 형태의 시간("HH:mm:ss" 등)을 LocalTime으로 파싱
 * null 또는 파싱 불가능하면 null 반환
 */
@RequiresApi(Build.VERSION_CODES.O)
fun parseTime(timeStr: String?): LocalTime? {
    return try {
        if (!timeStr.isNullOrEmpty()) {
            LocalTime.parse(timeStr)
        } else null
    } catch (e: Exception) {
        null
    }
}

/**
 * 문자열 형태의 날짜("yyyy-MM-dd" 등)을 LocalDate로 파싱
 * null 또는 파싱 불가능하면 null 반환
 */
@RequiresApi(Build.VERSION_CODES.O)
fun parseDate(dateStr: String?): LocalDate? {
    return try {
        if (!dateStr.isNullOrEmpty()) {
            LocalDate.parse(dateStr)
        } else null
    } catch (e: Exception) {
        null
    }
}
