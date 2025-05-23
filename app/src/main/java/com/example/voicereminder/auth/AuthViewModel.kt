package com.example.voicereminder.auth

import android.app.AlertDialog
import android.net.http.HttpException
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.MyFirebaseMessagingService
import com.example.voicereminder.model.User
import com.example.voicereminder.model.registerUser
import com.example.voicereminder.network.ApiService
import com.example.voicereminder.utils.TokenManager
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.net.SocketTimeoutException


class AuthViewModel(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val myFirebaseMessagingService: MyFirebaseMessagingService = MyFirebaseMessagingService()
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)//로그아웃을 구현하기 위함
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn//로그인 상태를 mainactivity에서 확인할 수 있음
    private var userId: Int? = null

    init {
        _isLoggedIn.value = tokenManager.getAccessToken() != null
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val username: String) : AuthState()
        data class Error(val message: String) : AuthState()
        data class ShowPopup(val message: String) : AuthState() // 팝업 상태 추가
    }
    // 새로운 에러 상태를 설정하는 함수
    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun register(user: registerUser) {
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
    fun setUserId(id: Int) {
        userId = id
    }

    fun getUserId(): Int? {
        return userId
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    fun login(user: User) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.login(user)//장고db에서 user정보를 가져와라 반환의 구조는 authresponse형태로
                if (response.isSuccessful) {//파이썬에서 값을 가져오는 것을 성공했다면.
                    response.body()?.let {
                        val accessToken = it.access //이게 문제였음 beaer한번더 적음
                        val refreshToken = it.refresh//여기서 authresponse의 형태로 받아온 값을 사용하는 것이다.
                        val username = it.username
                        setUserId(it.id)

                        val isLoggedIn = it.isLoggedIn // 서버에서 반환된 값 확인
                        Log.d("Login", "Access Token: $accessToken")
                        Log.d("Login", "Refresh Token: $refreshToken")
                        // 사용자 ID 저장
                        tokenManager.saveUserId(username)

                        tokenManager.saveAccessToken(accessToken)
                        tokenManager.saveRefreshToken(refreshToken)

                        // FCM 토큰을 서버에 전송

                        try {
                            val token = Firebase.messaging.token.await()
                            Log.d("FCMtoken", "fcm_ Token: $token")
                            myFirebaseMessagingService.sendTokenToServer(token, accessToken) // accesstoken으로 전송을 해준다.
                        } catch (e: Exception) {
                            Log.e("FCM", "FCM 토큰 생성 실패", e)
                        }



                        _authState.value = AuthState.Success(it.username)
                    }
                }
                else if (response.code() == 403) {  // 다른 기기에서 이미 로그인된 경우 실행되는 코드 에러 메세지 팝업을 띄운다.
                    response.errorBody()?.string()?.let { errorMessage ->
                        _authState.value = AuthState.ShowPopup(errorMessage)
                    }
                }
                else {
                    _authState.value = AuthState.Error("로그인 실패: ${response.code()}")
                }
            }
//            catch (e: Exception) {
//                Log.d("network problem","fuc",e)
//                _authState.value = AuthState.Error("네트워크 오류: ${e.message}")
//            }
            catch (e: HttpException) {
                Log.e("Login", "HTTP 예외", e)
                _authState.value = AuthState.Error("HTTP 오류: ${e}")
            } catch (e: SocketTimeoutException) {
                Log.e("Login", "소켓 타임아웃", e)
                _authState.value = AuthState.Error("서버 응답 시간 초과")
            } catch (e: IOException) {
                Log.e("Login", "네트워크 IO 예외", e)
                _authState.value = AuthState.Error("네트워크 연결 오류")
            } catch (e: Exception) {
                Log.e("Login", "알 수 없는 오류", e)
                _authState.value = AuthState.Error("알 수 없는 오류: ${e.message}")
            }
        }
    }


    // 로그인하고 로그아웃 상태를 확인하는 코드
    fun setLoggedIn(value: Boolean) {
        _isLoggedIn.value = value
    }
    fun setIdle() {
        _authState.value = AuthState.Idle // 상태 초기화 함수 추가
    }

    fun logout(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Idle // 로그아웃 시 상태 초기화
                onSuccess()
                val accessToken = tokenManager.getAccessToken()
                val refreshToken = tokenManager.getRefreshToken() // Refresh 토큰 가져오기
                Log.d("Logout11", "Access Token: $accessToken")
                Log.d("Logout22", "Access Token: $refreshToken")
                if (accessToken == null) {
                    onError("로그아웃 실패: 토큰이 없습니다.")
                    return@launch
                }

                var response = apiService.logout( "Bearer $accessToken",
                    mapOf("refresh" to refreshToken)   ) // Refresh 토큰 포함)

                Log.d("Logout333", "Response received: ${response.raw()}")

                if (!response.isSuccessful && response.code() == 401) {
                    // 액세스 토큰이 만료된 경우 리프레시 토큰으로 새로운 액세스 토큰 요청
                    val refreshResponse = apiService.refreshToken(mapOf("refresh" to refreshToken))
                    if (refreshResponse.isSuccessful) {
                        val newAccessToken = refreshResponse.body()?.get("access") as? String
                        if (newAccessToken != null) {
                            tokenManager.saveAccessToken(newAccessToken)
                            // 새로운 액세스 토큰으로 로그아웃 재시도
                            response = apiService.logout("Bearer $newAccessToken", mapOf("refresh" to refreshToken))
                        } else {
                            onError("새 액세스 토큰을 받지 못했습니다.")
                            return@launch
                        }
                    } else {
                        // 리프레시 토큰도 만료된 경우
                        onError("세션이 만료되었습니다. 다시 로그인 해주세요.")
                        // 로그인 화면으로 리디렉션하는 로직 추가
                        _authState.value = AuthState.Idle // 로그아웃 시 상태 초기화
                        onSuccess()
//                        redirectToLoginScreen()
//                        return@launch
                    }
                }

                if (response.isSuccessful) {
                    tokenManager.clearTokens() // 로컬에 저장된 토큰 삭제
                    Firebase.messaging.deleteToken()//재성성되지 않게 토큰도 삭제
                    _authState.value = AuthState.Idle // 로그아웃 시 상태 초기화
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


    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val accessToken = tokenManager.getAccessToken()
                val refreshToken = tokenManager.getRefreshToken()
                if (accessToken == null) {
                    onError("토큰이 없습니다.")
                    return@launch
                }

                val response = apiService.deleteAccount( "Bearer $accessToken",
                    mapOf("refresh" to refreshToken)   )

                if (response.isSuccessful) {
                    tokenManager.clearTokens()
                    _authState.value = AuthState.Idle
                    onSuccess()
                } else {
                    onError("계정 삭제 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                onError("네트워크 오류: ${e.message}")
            }
        }
    }


    fun updateFcmToken(fcmToken: String) {
        viewModelScope.launch {
            try {
                val response = apiService.updateFcmToken(
                    "Bearer ${tokenManager.getAccessToken()}",
                    mapOf("fcm_token" to fcmToken)
                )
               
                if (response.isSuccessful) {
                    // FCM 토큰 업데이트 성공
                } else {
                    // FCM 토큰 업데이트 실패
                }
            } catch (e: Exception) {
                // 네트워크 오류 등
            }
        }
    }


}