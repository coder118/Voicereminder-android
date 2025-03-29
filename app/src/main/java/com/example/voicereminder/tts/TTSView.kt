package com.example.voicereminder.tts

import android.content.Context


// 파일 경로: tts/TTSViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TTSView : ViewModel() {
//    fun playTTS(context: Context, text: String) {
//        viewModelScope.launch {
//            TTSHelper.synthesizeText(context, text)?.let { file ->
//                // MediaPlayer 구현부
//            }
//        }
//    }
    private var mediaPlayer: MediaPlayer? = null

    fun playTTS(context: Context, text: String) {
        viewModelScope.launch {
            Log.d("playtts","working")
            // 1. 텍스트를 음성 파일로 변환
            val audioFile = withContext(Dispatchers.IO) {
                TTSHelper.synthesizeText(context, text)
            }

            // 2. 음성 파일 재생
            audioFile?.let { file ->
                playAudioFile(context, file)
            }
        }
    }

    private fun playAudioFile(context: Context, file: File) {
        try {
            Log.d("playaudiofile","play wokring")
            // 기존 재생 중인 음성 정리
            releaseMediaPlayer()

            // 새 MediaPlayer 인스턴스 생성
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                prepareAsync() // 비동기 준비

                setOnPreparedListener { mp ->
                    mp.start() // 준비 완료 시 재생 시작
                }

                setOnCompletionListener {
                    releaseMediaPlayer()
                    file.delete() // 재생 완료 후 임시 파일 삭제
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            releaseMediaPlayer()
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        releaseMediaPlayer() // ViewModel 종료 시 리소스 정리
    }
}
//class TTSView {
//    private val TAG = "TtsInitializer"
//    private val JSON_FILE_NAME = "voicereminder_app_d9862bebb234.json" // 파일 이름
//
//    fun initializeTtsClient(context: Context): TextToSpeechClient? {
//        return try {
//            val credentials = loadCredentials(context)
//            TextToSpeechClient.create(
//                TextToSpeechSettings.newBuilder()
//                    .setCredentialsProvider { credentials }
//                    .build()
//            )
//        } catch (e: Exception) {
//            Log.e(TAG, "TTS 클라이언트 초기화 실패: ${e.message}", e)
//            null
//        }
//    }
//
//    private fun loadCredentials(context: Context): Credentials {
//        val inputStream: InputStream = context.resources.openRawResource(
//            context.resources.getIdentifier(
//                JSON_FILE_NAME.removeSuffix(".json"),
//                "raw",
//                context.packageName
//            )
//        )
//
//        return GoogleCredentials.fromStream(inputStream)
//    }
//}