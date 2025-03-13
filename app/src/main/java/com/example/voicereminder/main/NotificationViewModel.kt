package com.example.voicereminder.main


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.model.NotificationResponse
import com.example.voicereminder.network.ApiService
import com.example.voicereminder.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    // 알림 상태를 관리하기 위한 StateFlow
    private val _notificationsState = MutableStateFlow<NotificationsState>(NotificationsState.Idle)
    val notificationsState: StateFlow<NotificationsState> = _notificationsState

    // 상태 클래스 정의 (Idle, Loading, Success, Error)
    sealed class NotificationsState {
        object Idle : NotificationsState() // 초기 상태
        object Loading : NotificationsState() // 로딩 중 상태
        data class Success(val notifications: List<NotificationResponse>) : NotificationsState() // 성공 상태
        data class Error(val message: String) : NotificationsState() // 에러 상태
    }

    /**
     * 알림 데이터를 서버에서 가져오는 함수
     */
    fun checkNotifications() {
        viewModelScope.launch {
            _notificationsState.value = NotificationsState.Loading // 로딩 상태로 변경

            try {
                // 토큰 가져오기
                val token = "Bearer ${tokenManager.getAccessToken()}"

                // API 호출
                val response = apiService.checkNotifications(token)

                if (response.isSuccessful) {
                    // 응답 성공 시 처리
                    response.body()?.let { notifications ->
                        if (notifications.isNotEmpty()) {
                            _notificationsState.value = NotificationsState.Success(notifications)
                        } else {
                            _notificationsState.value = NotificationsState.Error("알림이 없습니다.")
                        }
                    } ?: run {
                        _notificationsState.value = NotificationsState.Error("응답 데이터가 없습니다.")
                    }
                } else {
                    // 응답 실패 시 처리 (예: 401 Unauthorized, 500 Internal Server Error 등)
                    when (response.code()) {
                        401 -> _notificationsState.value = NotificationsState.Error("인증 실패: 다시 로그인하세요.")
                        500 -> _notificationsState.value = NotificationsState.Error("서버 오류가 발생했습니다.")
                        else -> _notificationsState.value =
                            NotificationsState.Error("알림 조회 실패: ${response.code()} - ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                // 네트워크 오류 또는 기타 예외 처리
                Log.e("NotificationViewModel", "알림 조회 중 오류 발생", e)
                _notificationsState.value = NotificationsState.Error("네트워크 오류: ${e.message}")
            }
        }
    }
}
