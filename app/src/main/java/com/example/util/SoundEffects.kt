package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

object SoundEffects {

    private const val TAG = "SoundEffects"
    var isSoundEnabled: Boolean = true

    private val audioScope = CoroutineScope(Dispatchers.Default)

    /**
     * Plays a pleasant gentle pop sound when a tile is tapped.
     */
    fun playTilePop() {
        if (!isSoundEnabled) return
        audioScope.launch {
            playToneSweep(startFreq = 580f, endFreq = 920f, durationMs = 60, amplitude = 0.4f)
        }
    }

    /**
     * Plays a cheerful 3-note ascending arpeggio when a triple match occurs.
     */
    fun playTripleMatch() {
        if (!isSoundEnabled) return
        audioScope.launch {
            playTone(frequency = 523.25f, durationMs = 70, amplitude = 0.5f) // C5
            kotlinx.coroutines.delay(65)
            playTone(frequency = 659.25f, durationMs = 70, amplitude = 0.5f) // E5
            kotlinx.coroutines.delay(65)
            playTone(frequency = 783.99f, durationMs = 120, amplitude = 0.55f) // G5
        }
    }

    /**
     * Plays a victory celebration fanfare when a level is cleared.
     */
    fun playVictory() {
        if (!isSoundEnabled) return
        audioScope.launch {
            val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f)
            for (note in notes) {
                playTone(frequency = note, durationMs = 90, amplitude = 0.45f)
                kotlinx.coroutines.delay(85)
            }
            playTone(frequency = 1046.50f, durationMs = 240, amplitude = 0.6f)
        }
    }

    /**
     * Plays an alert/buzz tone when the tray is full or action failed.
     */
    fun playTrayFullAlert() {
        if (!isSoundEnabled) return
        audioScope.launch {
            playTone(frequency = 280f, durationMs = 120, amplitude = 0.4f)
            kotlinx.coroutines.delay(100)
            playTone(frequency = 220f, durationMs = 160, amplitude = 0.4f)
        }
    }

    /**
     * Plays a sparkling high ping when coins are awarded.
     */
    fun playCoinReward() {
        if (!isSoundEnabled) return
        audioScope.launch {
            playTone(frequency = 1318.5f, durationMs = 70, amplitude = 0.45f) // E6
            kotlinx.coroutines.delay(60)
            playTone(frequency = 1567.98f, durationMs = 140, amplitude = 0.5f) // G6
        }
    }

    /**
     * Plays a swoosh / booster activation sound.
     */
    fun playBoosterUse() {
        if (!isSoundEnabled) return
        audioScope.launch {
            playToneSweep(startFreq = 400f, endFreq = 1200f, durationMs = 140, amplitude = 0.45f)
        }
    }

    private fun playTone(frequency: Float, durationMs: Int, amplitude: Float = 0.5f) {
        val sampleRate = 22050
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val time = i.toDouble() / sampleRate
            val envelope = when {
                i < numSamples * 0.1f -> i / (numSamples * 0.1f)
                i > numSamples * 0.8f -> (numSamples - i) / (numSamples * 0.2f)
                else -> 1f
            }
            val angle = 2.0 * PI * frequency * time
            buffer[i] = (sin(angle) * amplitude * envelope * Short.MAX_VALUE).toInt().toShort()
        }

        runAudioTrack(buffer, sampleRate)
    }

    private fun playToneSweep(startFreq: Float, endFreq: Float, durationMs: Int, amplitude: Float = 0.4f) {
        val sampleRate = 22050
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val curFreq = startFreq + (endFreq - startFreq) * progress
            val time = i.toDouble() / sampleRate
            val envelope = when {
                i < numSamples * 0.15f -> i / (numSamples * 0.15f)
                i > numSamples * 0.7f -> (numSamples - i) / (numSamples * 0.3f)
                else -> 1f
            }
            val angle = 2.0 * PI * curFreq * time
            buffer[i] = (sin(angle) * amplitude * envelope * Short.MAX_VALUE).toInt().toShort()
        }

        runAudioTrack(buffer, sampleRate)
    }

    private fun runAudioTrack(buffer: ShortArray, sampleRate: Int) {
        var track: AudioTrack? = null
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSizeBytes = maxOf(buffer.size * 2, if (minBufSize > 0) minBufSize else 2048)

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSizeBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.write(buffer, 0, buffer.size)
                track.play()
                val sleepDuration = (buffer.size.toFloat() / sampleRate * 1000).toLong() + 15
                Thread.sleep(sleepDuration)
            }
        } catch (e: Throwable) {
            // Emulators or headless environments without audio hardware gracefully continue without crashing
            Log.v(TAG, "Audio synthesis unavailable: ${e.message}")
        } finally {
            try {
                if (track?.state == AudioTrack.STATE_INITIALIZED) {
                    track.stop()
                }
                track?.release()
            } catch (_: Throwable) {}
        }
    }
}
