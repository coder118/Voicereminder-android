package com.example.voicereminder

import android.app.Application
import com.example.voicereminder.main.SentenceViewModel
import com.example.voicereminder.network.RetrofitInstance
import com.example.voicereminder.utils.TokenManager

// 1. Custom Application 클래스 생성
class MyApp : Application() {
    lateinit var tokenManager: TokenManager
    lateinit var sentenceViewModel: SentenceViewModel

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
        sentenceViewModel = SentenceViewModel(apiService = RetrofitInstance.apiService, tokenManager = tokenManager)
    }
}