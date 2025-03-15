package com.example.voicereminder.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {//
    private const val BASE_URL = "https://10.0.2.2:8000" // 에뮬레이터에서 로컬 서버 접근

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // 연결 타임아웃 30초
        .readTimeout(30, TimeUnit.SECONDS)     // 읽기 타임아웃 30초
        .writeTimeout(30, TimeUnit.SECONDS)    // 쓰기 타임아웃 30초
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}