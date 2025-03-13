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
        data class Success(val notificationResponse: NotificationResponse) : SentenceState()
        data class Error(val message: String) : SentenceState()
    }

    suspend fun updateUserSettings(ttsVoiceId: Int, vibrationEnabled: Boolean): Boolean {
        return try {
            val token = "Bearer ${tokenManager.getAccessToken()}"
            val response = apiService.updateUserSettings(
                token,
                UserSettings(ttsVoiceId, vibrationEnabled)
            )
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
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

            // 1. 사용자 설정 먼저 업데이트
            val settingsUpdated = updateUserSettings(ttsVoiceId, vibrationEnabled)

            if (!settingsUpdated) {
                _sentenceState.value = SentenceState.Error("사용자 설정 업데이트 실패")
                return@launch
            }

            // 2. 문장 생성 요청
            try {
                val token = "Bearer ${tokenManager.getAccessToken()}"
                val notificationSettings = NotificationSettings(
                    id = 0, // 서버에서 생성될 ID
                    username = "", // 서버에서 설정될 사용자 이름
                    repeat_mode = if (isRandom) "random" else "once",
                    notification_time = time,
                    notification_date = date,
                    notification_count = 1
                )
                //장고에서 값을 저장하고 돌아올때 값을 반환?
                val response = apiService.createSentence(
                    token,
                    mapOf(
                        "content" to content,
                        "notification_settings" to notificationSettings
                    )
                )

                if (response.isSuccessful) {//장고에서 값을 받아오는데 성공하면 실행 데이터의 구조는 notificationResponse
                    response.body()?.let { notificationResponse ->
                        _sentenceState.value = SentenceState.Success(notificationResponse)//unit으로 response를 받기 때문에 이렇게 값을 반환받을 이유가 없다. 임시로 수정을 함
                    } ?: run {
                        _sentenceState.value = SentenceState.Error("응답 데이터가 없습니다.")
                    }
                } else {
                    _sentenceState.value = SentenceState.Error("문장 저장 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                _sentenceState.value = SentenceState.Error("네트워크 오류: ${e.message}")
            }
        }
    }
}


