package com.whisk.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class VoiceRecordingState {
    IDLE, LISTENING, PROCESSING, COMPLETED, ERROR
}

class VoiceNoteManager(
    private val context: Context,
    private val notesRepository: NotesRepository,
) {

    private val _recordingState = MutableStateFlow(VoiceRecordingState.IDLE)
    val recordingState: StateFlow<VoiceRecordingState> = _recordingState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    fun startListening(onError: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _recordingState.value = VoiceRecordingState.ERROR
            onError("Speech recognition service is not available on this device.")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _recordingState.value = VoiceRecordingState.LISTENING
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _recordingState.value = VoiceRecordingState.PROCESSING
                    }
                    override fun onError(error: Int) {
                        _recordingState.value = VoiceRecordingState.ERROR
                        onError("Recognition error code: $error")
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        _liveTranscript.value = text
                        _recordingState.value = VoiceRecordingState.COMPLETED
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let {
                            _liveTranscript.value = it
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _recordingState.value = VoiceRecordingState.ERROR
            onError("Failed to start speech listener: ${e.message}")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    suspend fun saveTranscriptAsNote(): Note = withContext(Dispatchers.IO) {
        val transcriptText = _liveTranscript.value.trim()
        val now = Date()
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(now)
        val title = "Voice Memo - $dateStr"

        // Extract action items if text mentions "todo", "need to", "remember to"
        val lines = transcriptText.split(Regex("[.!?]")).map { it.trim() }.filter { it.isNotBlank() }
        val actionItems = mutableListOf<String>()
        lines.forEach { sentence ->
            val lower = sentence.lowercase()
            if (lower.startsWith("todo") || lower.startsWith("need to") || lower.startsWith("remember to") || lower.contains("action item")) {
                actionItems.add("- [ ] $sentence")
            }
        }

        val body = buildString {
            append("# ").append(title).append("\n\n")
            append("> Transcribed from voice audio on ").append(dateStr).append("\n\n")
            append("## Transcript\n\n")
            append(transcriptText.ifBlank { "No audio recorded." }).append("\n\n")
            if (actionItems.isNotEmpty()) {
                append("## Extracted Action Items\n\n")
                actionItems.forEach { append(it).append("\n") }
                append("\n")
            }
        }

        val note = Note(
            title = title,
            body = body,
            tags = listOf("voice-memo", "audio"),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        notesRepository.saveNote(note)
        _liveTranscript.value = ""
        _recordingState.value = VoiceRecordingState.IDLE
        note
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
