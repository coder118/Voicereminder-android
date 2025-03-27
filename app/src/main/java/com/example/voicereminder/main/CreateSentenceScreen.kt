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
import com.example.voicereminder.model.TTSVoiceResponse
import com.example.voicereminder.model.UserSettings
import java.time.LocalDate
import java.time.LocalTime
import java.time.Instant
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class) // 실험적 API 사용 선언
@Composable
fun CreateSentenceScreen(
    sentenceViewModel: SentenceViewModel,
    onCancel: () -> Unit,
    onSubmitSuccess: () -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var isRandom by remember { mutableStateOf(false) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var ttsVoiceId by remember { mutableStateOf(0) } // TTS 음성 ID 선택

    val sentenceState by sentenceViewModel.sentenceState.collectAsState()

    // TimePicker와 DatePicker 상태 생성
//    val timeInputState = rememberTimeInputState()
    val timePickerState = rememberTimePickerState()
    // 선택된 시간과 날짜를 업데이트하는 로직
    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
    }


    val todayMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = todayMillis,
        selectableDates = object : SelectableDates {//오늘이전의 날짜값은 선택이 되지 않는다.
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {

                // UTC 밀리초를 현재 시간대의 날짜로 변환
                val selectedDate = Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                return !selectedDate.isBefore(LocalDate.now()) // 오늘 포함 이후 날짜만 선택 가능

//                return utcTimeMillis >= todayMillis
            }
        }
    )

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()

        }
    }
    LaunchedEffect(Unit) {
        sentenceViewModel.loadTTSVoices()
    }

    DisposableEffect(Unit) {
        onDispose {
            sentenceViewModel.resetState() // 화면이 꺼질때 원래 상태로 초기화
        }
    }

    // UI 구성Modifier.fillMaxSize()
    Column(modifier =Modifier.padding(16.dp) ) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // 문장 입력 필드
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("문장을 입력하세요...") }
                )
            }

            item {
                // 시간 및 날짜 선택 (랜덤 설정이 아닐 경우)
                if (!isRandom) {
                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                // 랜덤 알람 설정 토글
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Switch(
                        checked = isRandom,
                        onCheckedChange = { isRandom = it }
                    )
                    Text("랜덤 알람 설정")
                }
            }

            item {
                // 진동 활성화 토글
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it }
                    )
                    Text("진동 활성화")
                }
            }

            item {
                // TTS 음성 ID 선택
//                TextField(
//                    value = ttsVoiceId.toString(),
//                    onValueChange = { ttsVoiceId = it.toIntOrNull() ?: 0 },
//                    modifier = Modifier.fillMaxWidth(),
//                    placeholder = { Text("TTS 음성 ID를 입력하세요") }
//                )
                TTSSoundSelector(
                    sentenceviewModel = sentenceViewModel,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                // 하단 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = onCancel) {
                        Text("취소")
                    }
                    Button(onClick = {
                        sentenceViewModel.createSentence(
                            content = content,
                            time = selectedTime?.toString(),
                            date = selectedDate?.toString(),
                            isRandom = isRandom,

                            vibrationEnabled = vibrationEnabled
                        )
                    }) {
                        Text("확인")
                    }
                }
            }
        }



        // 상태에 따른 처리 결과 표시
        when (sentenceState) {
            is SentenceViewModel.SentenceState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            is SentenceViewModel.SentenceState.Success -> {
                LaunchedEffect(Unit) {
                    onSubmitSuccess() // 성공 시 콜백 호출
                    sentenceViewModel.resetState() //즉시 초기화
                }
                Text("문장이 성공적으로 생성되었습니다!", color = MaterialTheme.colorScheme.primary)
            }
            is SentenceViewModel.SentenceState.Error -> {
                val errorMessage = (sentenceState as SentenceViewModel.SentenceState.Error).message
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
            else -> {}
        }
    }




}

@Composable
fun TTSSoundSelector(
    sentenceviewModel: SentenceViewModel,
    modifier: Modifier = Modifier
) {
    val ttsVoices by sentenceviewModel.ttsVoices.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var selectedVoice by remember { mutableStateOf<TTSVoiceResponse?>(null) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedVoice?.name ?: "TTS 음성 선택")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ttsVoices.forEach { voice ->
                DropdownMenuItem(
                    text = { Text("${voice.name} (${voice.language})") },
                    onClick = {
                        selectedVoice = voice//voice를 이용을 해서 sentence data class선언한 형태에 접근이 가능
                        sentenceviewModel.selectTTSVoice(voice.id)//data class 에서 int로 정한 id값을 뷰파일로 보낸다.
                        expanded = false
                    }
                )
            }
        }
    }
}

//        // 문장 입력 필드
//        TextField(
//            value = content,
//            onValueChange = { content = it },
//            modifier = Modifier.fillMaxWidth(),
//            placeholder = { Text("문장을 입력하세요...") }
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // 시간 및 날짜 선택 (랜덤 설정이 아닐 경우)
//        if (!isRandom) {
//            TimePicker (
//                state = timePickerState,
//                modifier = Modifier.fillMaxWidth()
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//
//            DatePicker (
//                state = datePickerState,
//                modifier = Modifier.fillMaxWidth()
//            )
//        }
//
//        // 랜덤 알람 설정 토글
//        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
//            Switch(
//                checked = isRandom,
//                onCheckedChange = { isRandom = it }
//            )
//            Text("랜덤 알람 설정")
//        }
//
//        // 진동 활성화 토글
//        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
//            Switch(
//                checked = vibrationEnabled,
//                onCheckedChange = { vibrationEnabled = it }
//            )
//            Text("진동 활성화")
//        }
//
//        // TTS 음성 ID 선택 (예시로 간단한 숫자 입력 필드 사용)
//        TextField(
//            value = ttsVoiceId.toString(),
//            onValueChange = { ttsVoiceId = it.toIntOrNull() ?: 0 },
//            modifier = Modifier.fillMaxWidth(),
//            placeholder = { Text("TTS 음성 ID를 입력하세요") }
//        )
//
//        // 하단 버튼
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Button(onClick = onCancel) {
//                Text("취소")
//            }
//            Button(onClick = {
//                sentenceViewModel.createSentence(
//                    content = content,
//                    time = selectedTime?.toString(),
//                    date = selectedDate?.toString(),
//                    isRandom = isRandom,
//                    ttsVoiceId = ttsVoiceId,
//                    vibrationEnabled = vibrationEnabled
//                )
//            }) {
//                Text("확인")
//            }
//        }Modifier
//                .weight(1f)  // 남은 공간을 모두 차지하도록 설정
//                .fillMaxWidth(),
//