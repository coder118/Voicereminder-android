package com.example.voicereminder.model

data class Sentence(
    val id: Int,
    val username: String,  // 사용자 ID 추가
    val content: String,
    val is_ai_generated: Boolean = false,
    val created_at: String
)

data class NotificationSettings(
    val id: Int,
    val username: String,  // 사용자 ID 추가
    val repeat_mode: String,
    val notification_time: String?,
    val notification_date: String?,
    val notification_count: Int = 1
)

data class UserSettings(//TTS 음성 ID와 진동 설정 여부를 포함
    val tts_voice: Int,
    val vibration_enabled: Boolean
)
//알람이 울릴때 적힌 문장, tts, 진동 여부, 알람시간의 값들을 한번에 받아오는 데이터 구조
data class NotificationResponse(//문장 생성 API 호출 후 Django가 안드로이드로 보내는 응답 구조
    val sentence: Sentence,
    val notificationSettings: NotificationSettings,
    val userSettings: UserSettings
)
