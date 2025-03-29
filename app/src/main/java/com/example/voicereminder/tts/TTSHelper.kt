package com.example.voicereminder.tts

// 파일 경로: tts/TTSHelper.kt
import android.content.Context
import com.example.voicereminder.R
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.texttospeech.v1.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object TTSHelper {
    suspend fun synthesizeText(
        context: Context,
        text: String,
        languageCode: String = "ko-KR"
    ): File? = withContext(Dispatchers.IO) {
        try { // 1. raw 폴더에서 서비스 계정 키 파일 읽기
            val credentialsStream = context.resources.openRawResource(R.raw.voicereminder_app_d9862bebb234)
            val credentials = GoogleCredentials.fromStream(credentialsStream)

            // 2. 인증된 클라이언트 생성
            TextToSpeechClient.create(
                TextToSpeechSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build()
            ).use { ttsClient ->
                val input = SynthesisInput.newBuilder().setText(text).build()
                val voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode(languageCode)
                    .build()
                val audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.MP3)
                    .build()

                val response = ttsClient.synthesizeSpeech(input, voice, audioConfig)
                val tempFile = File.createTempFile("tts_", ".mp3", context.cacheDir)
                tempFile.outputStream().use { it.write(response.audioContent.toByteArray()) }
                tempFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
