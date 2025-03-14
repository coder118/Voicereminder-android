package com.example.voicereminder.network

import retrofit2.http.POST
import retrofit2.http.Body

import com.example.voicereminder.model.AuthResponse
import com.example.voicereminder.model.*
import retrofit2.http.Header
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH

interface ApiService {
//    @GET("tests/")
//    fun getTests(): Call<List<Test>>
//
//    @POST("tests/")
//    fun createTest(@Body test: Test): Call<Test>

    @POST("register/")
    suspend fun register(@Body user: User): Response<Unit>

    @POST("login/")
    suspend fun login(@Body user: User): Response<AuthResponse>

    // (추가) 로그아웃 엔드포인트
    @POST("logout/")
    suspend fun logout(@Header("Authorization") token: String, @Body body: Map<String, String?>): Response<Unit>

    @POST("token/refresh/")//리프레쉬토큰 확인 코드
    suspend fun refreshToken(@Body body: Map<String, String?>): Response<Map<String, String>>

    //계정삭제 엔드 포인트
    @POST("delete-account/")
    suspend fun deleteAccount(@Header("Authorization") token: String, @Body body: Map<String, String?>): Response<Unit>

    // 사용자 설정 업데이트 엔드포인트
    @PATCH("user/settings/")//PATCH 메서드는 기존 리소스의 일부 속성을 업데이트할 때 사용
    suspend fun updateUserSettings(
        @Header("Authorization") token: String,
        @Body settings: UserSettings
    ): Response<Unit>

    // 문장 생성 엔드포인트
    @POST("sentences/create/")
    suspend fun createSentence(//만들어진 문장을 보낸다.
        @Header("Authorization") token: String,
        @Body data:  SentenceCreateRequest
    ): Response<Unit>//원래는 Unit으로 반환을 받아야 한다.

    @GET("notifications/check/")
    suspend fun checkNotifications(
        @Header("Authorization") token: String
    ): Response<List<NotificationResponse>>//생성된 문장이나 알림,진동,tts모든 정보를 받아온다. 위의 로그인 하는 것처럼 함수를 만들어서 it.000해서 사용이 가능해보인다.
}
