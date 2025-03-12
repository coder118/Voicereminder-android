package com.example.voicereminder.network

import retrofit2.http.POST
import retrofit2.http.Body

import com.example.voicereminder.model.AuthResponse
import com.example.voicereminder.model.User
import retrofit2.http.Header
import retrofit2.Response

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

    //계정삭제 엔드 포인트
    @POST("delete-account/")
    suspend fun deleteAccount(@Header("Authorization") token: String, @Body body: Map<String, String?>): Response<Unit>
}
