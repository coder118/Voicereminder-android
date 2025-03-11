package com.example.voicereminder.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.model.User
import com.example.voicereminder.network.ApiService
import com.example.voicereminder.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val username: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }
    // 새로운 에러 상태를 설정하는 함수
    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun register(user: User) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.register(user)
                if (response.isSuccessful) {
                    _authState.value = AuthState.Success(user.username)
                } else {
                    _authState.value = AuthState.Error("회원가입 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("네트워크 오류: ${e.message}")
            }
        }
    }

    fun login(user: User) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.login(user)
                if (response.isSuccessful) {
                    response.body()?.let {
                        val accessToken = "Bearer ${it.access}"
                        val refreshToken = it.refresh

                        Log.d("Login", "Access Token: $accessToken")
                        Log.d("Login", "Refresh Token: $refreshToken")

                        tokenManager.saveAccessToken(accessToken)
                        tokenManager.saveRefreshToken(refreshToken)


                        _authState.value = AuthState.Success(it.username)
                    }
                } else {
                    _authState.value = AuthState.Error("로그인 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("네트워크 오류: ${e.message}")
            }
        }
    }

    fun logout(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val accessToken = tokenManager.getAccessToken()
                val refreshToken = tokenManager.getRefreshToken() // Refresh 토큰 가져오기
                Log.d("Logout11", "Access Token: $accessToken")
                Log.d("Logout22", "Access Token: $refreshToken")
                if (accessToken == null) {
                    onError("로그아웃 실패: 토큰이 없습니다.")
                    return@launch
                }

                val response = apiService.logout("Bearer$accessToken",
                    mapOf("refresh" to refreshToken)   ) // Refresh 토큰 포함)

                Log.d("Logout333", "Response received: ${response.raw()}")

                if (response.isSuccessful) {
                    tokenManager.clearTokens() // 로컬에 저장된 토큰 삭제
                    onSuccess()
                } else {

                    val errorBody = response.errorBody()?.string()
                    Log.e("Logout", "Logout failed with code: ${response.code()}, message: ${response.message()}, error body: $errorBody")
                    Log.e("Logout", "Logout failed with code: ${response.code()}, message: ${response.message()}")
                    onError("로그아웃 실패: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                onError("네트워크 오류: ${e.localizedMessage ?: "알 수 없는 오류"}")
            }
        }
    }

}