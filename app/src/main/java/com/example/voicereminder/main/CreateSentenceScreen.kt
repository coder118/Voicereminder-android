package com.example.voicereminder.main

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.voicereminder.AlarmReceiver
import com.example.voicereminder.model.TTSVoiceResponse
import java.time.LocalDate
import java.time.LocalTime
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import android.content.Context

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
    val context = LocalContext.current // Context를 가져옴

    val sentenceState by sentenceViewModel.sentenceState.collectAsState()

    // TimePicker와 DatePicker 상태 생성
//    val timeInputState = rememberTimeInputState()
//    val timePickerState = rememberTimePickerState()
//    // 선택된 시간과 날짜를 업데이트하는 로직

    val timePickerState = rememberTimePickerState(
//        initialHour = Calendar.getInstance().get(Calendar.HOUR),
//        initialMinute = Calendar.getInstance().get(Calendar.MINUTE),
        is24Hour = false // 12시간제 설정
    )
    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
    }


    // 1. 시간대 변수 추출
    val zoneId = ZoneId.systemDefault()

// 2. 현재 날짜 계산
    val currentLocalDate = Instant.now().atZone(zoneId).toLocalDate()
    val initialDateMillis = currentLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

// 3. DatePickerState 초기화
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // UTC → LocalDate 변환
                val selectedDate = Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(zoneId)
                    .toLocalDate()
                return !selectedDate.isBefore(currentLocalDate) // 오늘 포함 이후 날짜만 선택 가능
            }
        }
    )

// 4. 선택된 날짜 상태 업데이트
    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { utcMillis ->
            selectedDate = Instant.ofEpochMilli(utcMillis).atZone(zoneId).toLocalDate()
        }
    }


    // 3. 선택된 시간+날짜를 합쳐 Calendar로 변환
    fun getAlarmCalendar(): Calendar {
        return Calendar.getInstance().apply {
            // DatePicker에서 선택한 날짜 적용
            Log.d("getAlarmCalendar", "getAlarmCalendar getAlarmCalendar:")
            datePickerState.selectedDateMillis?.let {millis ->
                timeInMillis = millis
                // 월 값 보정 (0 → 1월, 1 → 2월 ...)
                val selectedMonth = get(Calendar.MONTH)
                set(Calendar.MONTH, selectedMonth+1) // 실제로는 보정 필요 없음 (내부 처리용)

                // TimePicker의 시간을 추가로 적용
                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                set(Calendar.MINUTE, timePickerState.minute)
                set(Calendar.SECOND, 0)
            }

            // 디버깅용 로그 (보정된 월 표시)
            Log.d("Alarm", "보정된 월: ${get(Calendar.MONTH) + 1}월")
            // 이미 지난 시간인 경우 다음 날로 조정
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
    }

    // 4. 알람 설정 함수 이걸 메인액티비티로 보내야 함
    fun setAlarm() {
        Log.d("setAlarm", "setAlarm setAlarm:")
        val calendar = getAlarmCalendar()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("MESSAGE", "알람이 울립니다!")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Doze 모드 대응
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val clockInfo = AlarmManager.AlarmClockInfo(
                calendar.timeInMillis,
                pendingIntent // 알람 아이콘 클릭 시 실행될 PendingIntent
            )
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }


    }

//    val todayMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

//    val utcNow = Instant.now()
//    val localNow = ZonedDateTime.now(ZoneId.systemDefault())
//
//    Log.d("DEBUG", "UTC Now: $utcNow")
//    Log.d("DEBUG", "Local Now: $localNow")
////    UTC Now: 2025-04-03T07:01:08.798Z
////    2025-04-03 16:01:08.798  2170-2170  DEBUG
////    com.example.voicereminder            D  Local Now: 2025-04-03T16:01:08.798+09:00[Asia/Seoul]
//    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = todayMillis,
//        selectableDates = object : SelectableDates {//오늘이전의 날짜값은 선택이 되지 않는다.
//            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
//
//                // UTC 밀리초를 현재 시간대의 날짜로 변환
//                val selectedDate = Instant.ofEpochMilli(utcTimeMillis)
//                    .atZone(ZoneId.systemDefault())
//                    .toLocalDate()
//                return !selectedDate.isBefore(LocalDate.now()) // 오늘 포함 이후 날짜만 선택 가능
//
////                return utcTimeMillis >= todayMillis
//            }
//        }
//    )

//    LaunchedEffect(datePickerState.selectedDateMillis) {
//        datePickerState.selectedDateMillis?.let {
//            selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
//
//        }
//    }

    LaunchedEffect(Unit) {
        sentenceViewModel.loadTTSVoices()
    }

    DisposableEffect(Unit) {
        onDispose {
            sentenceViewModel.resetState() // 화면이 꺼질때 원래 상태로 초기화
        }
    }

    val insets = WindowInsets.systemBars.asPaddingValues() //최신폰 네비게이션 바 영역 침범 방지


    // UI 구성Modifier.fillMaxSize() Modifier.padding(16.dp)
    Column(modifier =Modifier
        .fillMaxSize()
        .padding(insets) // 시스템 UI 영역을 제외한 공간만 사용
        .padding(16.dp)) {

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


                    TimeInput(state = timePickerState, modifier = Modifier.fillMaxWidth())


//                    TimePicker(
//                        state = timePickerState,
//                        modifier = Modifier.fillMaxWidth()
//                    )
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
                    Text("매일 알람")
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
                        setAlarm()

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