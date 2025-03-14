package com.example.voicereminder.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.model.*
import com.example.voicereminder.network.ApiService
import com.example.voicereminder.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
class SentenceViewModel(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _sentenceState = MutableStateFlow<SentenceState>(SentenceState.Idle)
    val sentenceState: StateFlow<SentenceState> = _sentenceState

    sealed class SentenceState {
        object Idle : SentenceState()
        object Loading : SentenceState()
        object Success : SentenceState()  // Unit 응답으로 간소화
        data class Error(val message: String) : SentenceState()
    }

    fun createSentence(
        content: String,
        time: String?,
        date: String?,
        isRandom: Boolean,
        ttsVoiceId: Int,
        vibrationEnabled: Boolean
    ) {
        viewModelScope.launch {
            _sentenceState.value = SentenceState.Loading

            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val request = SentenceCreateRequest(//글쓰기화면에서 문장, 알림 정보, tts설정, 진동 유무의 값을 보냄
                    sentence = SentenceContent(content = content),

                    notificationSettings = NotificationSettings(
                        repeat_mode = if (isRandom) "random" else "once",
                        notification_time = time,
                        notification_date = date
                    ),

                    userSettings =UserSettings(
                        tts_voice = ttsVoiceId,
                        vibration_enabled = vibrationEnabled
                    )
                )

                val response = apiService.createSentence(token, request)

                if (response.isSuccessful) {
                    _sentenceState.value = SentenceState.Success
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _sentenceState.value = SentenceState.Error("저장 실패: $errorBody")
                }
            } catch (e: Exception) {
                _sentenceState.value = SentenceState.Error("네트워크 오류: ${e.message}")
            }
        }
    }
}

