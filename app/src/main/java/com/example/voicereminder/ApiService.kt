package com.example.voicereminder

import retrofit2.Call
import retrofit2.http.GET

    import retrofit2.http.POST
    import retrofit2.http.Body

interface ApiService {
    @GET("tests/")
    fun getTests(): Call<List<Test>>

    @POST("tests/")
    fun createTest(@Body test: Test): Call<Test>
}
