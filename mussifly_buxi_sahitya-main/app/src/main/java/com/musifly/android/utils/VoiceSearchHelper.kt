package com.musifly.android.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.musifly.android.R

class VoiceSearchHelper(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    fun startVoiceSearch(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError(context.getString(R.string.voice_search_not_available))
            return
        }
        
        if (isListening) {
            stopListening()
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                }
                
                override fun onBeginningOfSpeech() {
                    // Speech started
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    // Audio level changed
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {
                    // Audio buffer received
                }
                
                override fun onEndOfSpeech() {
                    isListening = false
                }
                
                override fun onError(error: Int) {
                    isListening = false
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> context.getString(R.string.voice_search_error_audio)
                        SpeechRecognizer.ERROR_CLIENT -> context.getString(R.string.voice_search_error_client)
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> context.getString(R.string.voice_search_error_permissions)
                        SpeechRecognizer.ERROR_NETWORK -> context.getString(R.string.voice_search_error_network)
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> context.getString(R.string.voice_search_error_network_timeout)
                        SpeechRecognizer.ERROR_NO_MATCH -> context.getString(R.string.voice_search_error_no_match)
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> context.getString(R.string.voice_search_error_busy)
                        SpeechRecognizer.ERROR_SERVER -> context.getString(R.string.voice_search_error_server)
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> context.getString(R.string.voice_search_error_timeout)
                        else -> context.getString(R.string.voice_search_error_unknown)
                    }
                    onError(errorMessage)
                }
                
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onResult(matches[0])
                    } else {
                        onError(context.getString(R.string.voice_search_error_no_match))
                    }
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    // Partial results received
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Event occurred
                }
            })
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_search_prompt))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: SecurityException) {
            onError(context.getString(R.string.voice_search_error_permissions))
        } catch (e: Exception) {
            onError(context.getString(R.string.voice_search_error_unknown))
        }
    }
    
    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }
    
    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }
}

@Composable
fun rememberVoiceSearchHelper(): VoiceSearchHelper {
    val context = LocalContext.current
    val helper = remember { VoiceSearchHelper(context) }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            helper.destroy()
        }
    }
    
    return helper
}

