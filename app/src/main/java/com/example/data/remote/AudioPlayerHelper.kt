package com.example.data.remote

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

class AudioPlayerHelper(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var androidTts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        androidTts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                androidTts?.language = Locale.US
                isTtsReady = true
            }
        }
    }

    fun playBase64Audio(
        base64Audio: String,
        mimeType: String,
        fallbackText: String,
        onComplete: () -> Unit = {}
    ) {
        try {
            val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            val outputFile = File(context.cacheDir, "gemini_tts_alert_${System.currentTimeMillis()}.wav")

            val fileBytes = if (mimeType.contains("pcm", ignoreCase = true)) {
                // Wrap raw PCM 24kHz 16-bit mono into valid WAV
                addWavHeader(audioBytes, 24000, 1, 16)
            } else {
                audioBytes
            }

            FileOutputStream(outputFile).use { it.write(fileBytes) }

            stopCurrent()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(outputFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    onComplete()
                    outputFile.delete()
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Failed to play Gemini audio, falling back to System TTS", e)
            speakWithSystemTts(fallbackText, onComplete)
        }
    }

    fun speakWithSystemTts(text: String, onComplete: () -> Unit = {}) {
        if (isTtsReady && androidTts != null) {
            androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "station_dispatch")
            onComplete()
        }
    }

    fun stopCurrent() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
            androidTts?.stop()
        } catch (_: Exception) {}
    }

    fun release() {
        stopCurrent()
        androidTts?.shutdown()
        androidTts = null
    }

    private fun addWavHeader(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size
        header.putShort(1.toShort()) // AudioFormat (1 = PCM)
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort((channels * bitsPerSample / 8).toShort()) // BlockAlign
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray())
        header.putInt(totalAudioLen)

        val wav = ByteArray(44 + pcmData.size)
        System.arraycopy(header.array(), 0, wav, 0, 44)
        System.arraycopy(pcmData, 0, wav, 44, pcmData.size)
        return wav
    }
}
