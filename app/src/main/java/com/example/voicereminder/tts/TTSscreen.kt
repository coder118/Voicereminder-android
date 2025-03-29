package com.example.voicereminder.tts

// 파일 경로: ui/ssss.kt
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*


@Composable
fun TTSscreen() {
    val context = LocalContext.current
    val viewModel: TTSView = viewModel()
    var text by remember { mutableStateOf("") }

    Column {
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("변환할 텍스트") }
        )
        Button(onClick = { viewModel.playTTS(context, text) }) {
            Text("음성 변환 실행")
        }
    }
}
