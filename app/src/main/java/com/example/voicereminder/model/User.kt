package com.example.voicereminder.model

data class User(
    val username: String,
    val password: String,
    val tts_voice: Int? = null,
    val vibration_enabled: Boolean = true
)

data class AuthResponse(
    val refresh: String,
    val access: String,
    val username: String,
    val tts_voice: String?,
    val vibration_enabled: Boolean
)
