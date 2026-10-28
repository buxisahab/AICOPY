package com.buxi.music.ui.component

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import java.util.Locale

enum class VoiceSearchState {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR
}

class VoiceSearchManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var onResult: ((String) -> Unit)? = null
    private var onStateChange: ((VoiceSearchState) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PermissionChecker.PERMISSION_GRANTED
    }

    fun startListening(
        onResult: (String) -> Unit,
        onStateChange: (VoiceSearchState) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasPermission()) {
            onError("Microphone permission not granted")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available")
            return
        }

        this.onResult = onResult
        this.onStateChange = onStateChange
        this.onError = onError

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("VoiceSearch", "Ready for speech")
                    onStateChange(VoiceSearchState.LISTENING)
                }

                override fun onBeginningOfSpeech() {
                    Log.d("VoiceSearch", "Beginning of speech")
                    onStateChange(VoiceSearchState.LISTENING)
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Can be used for waveform animation
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d("VoiceSearch", "End of speech")
                    onStateChange(VoiceSearchState.PROCESSING)
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        else -> "Unknown error"
                    }
                    Log.e("VoiceSearch", "Error: $errorMessage")
                    onStateChange(VoiceSearchState.ERROR)
                    this@VoiceSearchManager.onError?.invoke(errorMessage)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val result = matches[0]
                        Log.d("VoiceSearch", "Result: $result")
                        onStateChange(VoiceSearchState.IDLE)
                        this@VoiceSearchManager.onResult?.invoke(result)
                    } else {
                        onStateChange(VoiceSearchState.ERROR)
                        this@VoiceSearchManager.onError?.invoke("No results")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("VoiceSearch", "Error starting speech recognizer", e)
            onStateChange(VoiceSearchState.ERROR)
            onError("Failed to start voice recognition")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            onStateChange?.invoke(VoiceSearchState.IDLE)
        } catch (e: Exception) {
            Log.e("VoiceSearch", "Error stopping speech recognizer", e)
        }
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            onStateChange?.invoke(VoiceSearchState.IDLE)
        } catch (e: Exception) {
            Log.e("VoiceSearch", "Error canceling speech recognizer", e)
        }
    }
}

@Composable
fun rememberVoiceSearchManager(): VoiceSearchManager {
    val context = LocalContext.current
    val manager = remember { VoiceSearchManager(context) }
    
    DisposableEffect(Unit) {
        onDispose {
            manager.cancel()
        }
    }
    
    return manager
}
