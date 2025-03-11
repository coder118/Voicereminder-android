package com.example.voicereminder.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

import android.util.Log
class TokenManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    // 액세스 토큰 저장
    fun saveAccessToken(token: String) {
        Log.d("save access","good")
        prefs.edit() { putString("access_token", token) }
    }

    // 액세스 토큰 조회
    fun getAccessToken(): String? = prefs.getString("access_token", null)
    // Refresh 토큰 저장
    fun saveRefreshToken(token: String) {
        Log.d("save refresh","good")
        prefs.edit { putString("refresh_token", token) }
    }
    // Refresh 토큰 조회
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
    // 토큰 삭제 (로그아웃 시)
    fun clearTokens() {
        prefs.edit() { remove("access_token")
            remove("refresh_token") }
    }
}