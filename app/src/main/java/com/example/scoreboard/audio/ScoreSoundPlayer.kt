package com.example.scoreboard.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import java.io.Closeable
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class ScoreSoundPlayer(context: Context) : Closeable {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private var loaded = false
    private var released = false
    private var pendingPlayCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private val soundId: Int

    init {
        soundPool.setOnLoadCompleteListener { pool, sampleId, status ->
            val pending = synchronized(this) {
                loaded = status == 0
                val count = if (loaded) pendingPlayCount else 0
                pendingPlayCount = 0
                count
            }
            repeat(pending) { index ->
                handler.postDelayed(
                    {
                        if (!released) pool.play(sampleId, 1f, 1f, 1, 0, 1f)
                    },
                    index * 55L,
                )
            }
        }
        soundId = try {
            val soundFile = File(context.cacheDir, "score_tap_v1.wav")
            if (!soundFile.exists() || soundFile.length() != EXPECTED_FILE_SIZE) {
                createScoreSound(soundFile)
            }
            soundPool.load(soundFile.absolutePath, 1)
        } catch (error: Exception) {
            soundPool.release()
            throw error
        }
    }

    @Synchronized
    fun play() {
        if (released) return
        if (loaded) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        } else {
            pendingPlayCount = (pendingPlayCount + 1).coerceAtMost(8)
        }
    }

    @Synchronized
    override fun close() {
        released = true
        handler.removeCallbacksAndMessages(null)
        soundPool.release()
    }

    private fun createScoreSound(file: File) {
        val sampleRate = SAMPLE_RATE
        val sampleCount = SAMPLE_COUNT
        val dataSize = sampleCount * 2
        val wav = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        wav.put("RIFF".toByteArray())
        wav.putInt(36 + dataSize)
        wav.put("WAVE".toByteArray())
        wav.put("fmt ".toByteArray())
        wav.putInt(16)
        wav.putShort(1)
        wav.putShort(1)
        wav.putInt(sampleRate)
        wav.putInt(sampleRate * 2)
        wav.putShort(2)
        wav.putShort(16)
        wav.put("data".toByteArray())
        wav.putInt(dataSize)

        var noiseSeed = 0x13579BDF
        repeat(sampleCount) { index ->
            val time = index.toDouble() / sampleRate
            noiseSeed = noiseSeed * 1_664_525 + 1_013_904_223
            val noise = ((noiseSeed ushr 8) and 0xFFFF) / 32_767.5 - 1.0
            val sample =
                sin(2.0 * PI * 920.0 * time) * exp(-58.0 * time) * 0.58 +
                    sin(2.0 * PI * 1_720.0 * time) * exp(-82.0 * time) * 0.22 +
                    noise * exp(-105.0 * time) * 0.34
            wav.putShort((sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.96).toInt().toShort())
        }

        file.outputStream().use { it.write(wav.array()) }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val SAMPLE_COUNT = 3_307
        const val EXPECTED_FILE_SIZE = 44L + SAMPLE_COUNT * 2L
    }
}
