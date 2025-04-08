package com.example.voicereminder.network

import retrofit2.http.POST
import retrofit2.http.Body

import com.example.voicereminder.model.AuthResponse
import com.example.voicereminder.model.*
import okhttp3.ResponseBody
import retrofit2.http.Header
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiService {
//    @GET("tests/")
//    fun getTests(): Call<List<Test>>
//
//    @POST("tests/")
//    fun createTest(@Body test: Test): Call<Test>

    @POST("register/")
    suspend fun register(@Body user: registerUser): Response<Unit>

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

    @POST("sentences/create_sentence/")
    suspend fun createSentence(
        @Header("Authorization") token: String,
        @Body request: SentenceCreateRequest
    ): Response<NotificationResponse>

    @GET("tts-voices/list_voices/")//tts의 목록을 가져오는 api
    suspend fun getTTSVoices(
        @Header("Authorization") token: String
    ): Response<List<TTSVoiceResponse>>

    @GET("notifications/check/")
    suspend fun checkNotifications(
        @Header("Authorization") token: String
    ): Response<List<NotificationResponse>>

    // 문장 수정 (예: PATCH /sentences/{id}/ 로 가정)
    @PATCH("sentences/{id}/update_sentence/")
    suspend fun updateSentence(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body data: SentenceCreateRequest
    ): Response<Unit>

    // 문장 삭제 (예: DELETE /sentences/{id}/ 로 가정)
    @DELETE("sentences/{id}/delete_sentence/")
    suspend fun deleteSentence(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>


    @POST("update_fcm_token/")
    suspend fun updateFcmToken(
        @Header("Authorization") token: String,
        @Body fcmToken: Map<String, String>
    ): Response<Unit>

    //fcm을 받고 그 문장을 다시 tts로 전환시키기 위해 서버로 보내준다.
    @Streaming
    @GET("tts/sentence/")  // 경로 변경
    suspend fun getTTSByNotification(
        @Header("Authorization") token: String,
        @Query("sentence_id") sentenceId: Long,  // Query 파라미터로 전달
        @Query("user_id") userId: Long           // 필수 권한 검증용
    ): Response<ResponseBody>

    data class TTSResponse(
        val audio: String,  // Base64
        val notification_id: Long
    )
}
