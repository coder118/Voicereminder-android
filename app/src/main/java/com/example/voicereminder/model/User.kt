package com.example.voicereminder.model

data class User(
    val username: String,
    val password: String,
    //val tts_voice: Int? = 0,//이것도 자료형을 파이썬을 잘 안맞춰서 오류 발생
    val fcm_token: String? = null,
    val vibration_enabled: Boolean = true
)

data class registerUser(
    val username: String,
    val password: String,
    //val tts_voice: Int? = 0,//이것도 자료형을 파이썬을 잘 안맞춰서 오류 발생

    val vibration_enabled: Boolean = true
)


data class AuthResponse(
    val refresh: String,
    val access: String,
    val username: String,
    //val tts_voice: Int?,
    val vibration_enabled: Boolean
)
