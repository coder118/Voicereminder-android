package com.example.voicereminder.main

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.model.*
import com.example.voicereminder.network.ApiService
import com.example.voicereminder.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
class SentenceViewModel(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _sentenceState = MutableStateFlow<SentenceState>(SentenceState.Idle)
    val sentenceState: StateFlow<SentenceState> = _sentenceState

    private val _notificationList = MutableStateFlow<List<NotificationResponse>>(emptyList())
    val notificationList = _notificationList.asStateFlow()

//    private val _ttsVoices = mutableStateListOf<TTSVoiceResponse>()
//    val ttsVoices: List<TTSVoiceResponse> get() = _ttsVoices

    private val _ttsVoices = MutableStateFlow<List<TTSVoiceResponse>>(emptyList())
    val ttsVoices: StateFlow<List<TTSVoiceResponse>> = _ttsVoices.asStateFlow()

    private val _selectedTTSVoiceId = MutableStateFlow<Int>(0)
    val selectedTTSVoiceId: StateFlow<Int?> = _selectedTTSVoiceId.asStateFlow()


    sealed class SentenceState {
        object Idle : SentenceState()
        object Loading : SentenceState()
        object Success : SentenceState()  // Unit 응답으로 간소화
        data class Error(val message: String) : SentenceState()
    }


    // 음성 목록 로드
    fun loadTTSVoices() {
        viewModelScope.launch {
            try {
                val response = apiService.getTTSVoices("Bearer ${tokenManager.getAccessToken()}")
                if(response.isSuccessful) {
//                    _ttsVoices.clear()
//                    _ttsVoices.addAll(response.body() ?: emptyList())
                    _ttsVoices.value = response.body() ?: emptyList()
                }
            } catch(e: Exception) {
                // 에러 처리
            }
        }
    }

    // 음성 선택 처리
    fun selectTTSVoice(voiceId: Int) {
        // 선택된 voiceId를 저장하는 로직
        _selectedTTSVoiceId.value = voiceId
    }

    // -----------------------
    // 목록 불러오기
    // -----------------------
    fun getSentence(onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getAccessToken()
                val response = apiService.checkNotifications("Bearer $token")
                if (response.isSuccessful) {
                    response.body()?.let { list ->
                        _notificationList.value = list
                    }
                } else {
                    onError("알림 목록 가져오기 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                onError(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun createSentence(
        content: String,
        time: String?,
        date: String?,
        isRandom: Boolean,

        vibrationEnabled: Boolean
    ) {
        viewModelScope.launch {
            _sentenceState.value = SentenceState.Loading

            try {
                // 선택된 TTS 음성 ID 가져오기
                val ttsVoiceId = _selectedTTSVoiceId.value
                    ?: throw Exception("TTS 음성을 선택해주세요.")

                val token = tokenManager.getAccessToken()
                val request = SentenceCreateRequest(//글쓰기화면에서 문장, 알림 정보, tts설정, 진동 유무의 값을 보냄
                    sentence = SentenceContent(content = content,
                        tts_voice = ttsVoiceId),

                    notificationSettings = NotificationSettings(
                        repeat_mode = if (isRandom) "random" else "once",
                        notification_time = time,
                        notification_date = date
                    ),

                    userSettings =UserSettings(

                        vibration_enabled = vibrationEnabled
                    )
                )

                val response = apiService.createSentence("Bearer $token", request)

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

    fun resetState() { //문장을 생성하고 확인버튼으로 끌때 문장의 상태를 초기화시켜줌
        _sentenceState.value = SentenceState.Idle
    }


    // -----------------------
    // 문장 수정
    // -----------------------
    fun updateSentence(
        id: Int,
        content: String,
        time: String?,
        date: String?,
        isRandom: Boolean,

        vibrationEnabled: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _sentenceState.value = SentenceState.Loading

                val ttsVoiceId = _selectedTTSVoiceId.value
                Log.d("Initial TTS Voice ID","me${ttsVoiceId}")

//                val ttsVoiceId = selectedTTSVoiceId

                val request = SentenceCreateRequest(
                    sentence = SentenceContent(content = content,
                        tts_voice = ttsVoiceId),

                    notificationSettings = NotificationSettings(
                        repeat_mode = if (isRandom) "random" else "once",
                        notification_time = time,
                        notification_date = date
                    ),

                    userSettings =UserSettings(

                        vibration_enabled = vibrationEnabled
                    )
                )

                val token = tokenManager.getAccessToken()
                val response = apiService.updateSentence("Bearer $token", id, request)

                if (response.isSuccessful) {
                    _sentenceState.value = SentenceState.Success
                    onSuccess()
                    // 성공 후 목록 다시 불러오기
                    getSentence(onError = {})
                } else {
                    _sentenceState.value = SentenceState.Error("문장 수정 실패: ${response.code()}")
                    onError("문장 수정 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                _sentenceState.value = SentenceState.Error(e.message ?: "알 수 없는 오류")
                onError(e.message ?: "알 수 없는 오류")
            }
        }
    }


    // -----------------------
    // 문장 삭제
    // -----------------------
    fun deleteSentence(
        id: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {

                val token =tokenManager.getAccessToken()
                val response = apiService.deleteSentence("Bearer $token", id)
                Log.d("DeleteResponse", "Response: $response")
                Log.d("ResponseCode", "Response Code: ${response.code()}")
                if (response.isSuccessful || response.code() == 204) {
                    // 목록 새로고침
                    getSentence(onError = {})
                    onSuccess()
                } else {
                    Log.e("DeleteError", "Failed with code: ${response.code()}")
                    onError("문장 삭제 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("DeleteSentenceError", "Exception occurred: ${e.message}")
                onError(e.message ?: "알 수 없는 오류")
            }
        }
    }
    //문장을 수정할때 문장의 정확한 위치값을 찾기 위해서 메인엑티비티파일에서 사용
    fun getSentenceById(id: Int): NotificationResponse? {
        return notificationList.value.find { it.id == id }
    }

}

