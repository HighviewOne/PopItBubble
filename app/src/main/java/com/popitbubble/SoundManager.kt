package com.popitbubble

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.math.*
import kotlin.random.Random

class SoundManager(private val context: Context) {

    private val soundPool: SoundPool
    private val popSoundIds = mutableListOf<Int>()
    private val loadedSounds = mutableSetOf<Int>()
    private var currentIndex = 0

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedSounds.add(sampleId)
        }

        // Generate 4 pop sound variations
        for (i in 0 until 4) {
            generateAndLoadSound(i)
        }
    }

    private fun generateAndLoadSound(variation: Int) {
        val sampleRate = 22050
        val durationSec = 0.13
        val numSamples = (sampleRate * durationSec).toInt()
        val pcm = ShortArray(numSamples)

        // Each variation has slightly different pitch and character
        val decayRate = 20.0 + variation * 3.0
        val toneFreq = 90.0 + variation * 25.0
        val noiseMix = 0.65 - variation * 0.05
        val toneMix = 0.4 + variation * 0.05

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * decayRate)

            // White noise component
            val noise = (Random.nextDouble() * 2.0 - 1.0) * noiseMix * envelope

            // Tonal thump component
            val tone = sin(2.0 * PI * toneFreq * t) * toneMix * envelope

            // Initial click transient
            val click = if (i < 5) (1.0 - i / 5.0) * 0.3 else 0.0

            val sample = ((noise + tone + click) * 32767.0).toInt().coerceIn(-32767, 32767)
            pcm[i] = sample.toShort()
        }

        val wavBytes = buildWavFile(pcm, sampleRate)
        val file = java.io.File(context.cacheDir, "pop_$variation.wav")
        file.writeBytes(wavBytes)

        val id = soundPool.load(file.absolutePath, 1)
        if (id > 0) popSoundIds.add(id)
    }

    private fun buildWavFile(pcm: ShortArray, sampleRate: Int): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = (numChannels * bitsPerSample / 8).toShort()
        val dataSize = pcm.size * 2

        fun writeLE4(v: Int) = dos.write(byteArrayOf(
            (v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(),
            ((v shr 16) and 0xFF).toByte(), ((v shr 24) and 0xFF).toByte()
        ))
        fun writeLE2(v: Short) = dos.write(byteArrayOf(
            (v.toInt() and 0xFF).toByte(), ((v.toInt() shr 8) and 0xFF).toByte()
        ))

        dos.write("RIFF".toByteArray())
        writeLE4(36 + dataSize)
        dos.write("WAVE".toByteArray())
        dos.write("fmt ".toByteArray())
        writeLE4(16)
        writeLE2(1) // PCM
        writeLE2(numChannels.toShort())
        writeLE4(sampleRate)
        writeLE4(byteRate)
        writeLE2(blockAlign)
        writeLE2(bitsPerSample.toShort())
        dos.write("data".toByteArray())
        writeLE4(dataSize)
        for (s in pcm) {
            dos.write(s.toInt() and 0xFF)
            dos.write((s.toInt() shr 8) and 0xFF)
        }

        return baos.toByteArray()
    }

    fun playPop() {
        if (popSoundIds.isEmpty()) return
        val id = popSoundIds[currentIndex % popSoundIds.size]
        currentIndex++
        if (id in loadedSounds) {
            // Slight pitch variation for natural feel
            val pitch = 0.85f + Random.nextFloat() * 0.3f
            val vol = 0.8f + Random.nextFloat() * 0.2f
            soundPool.play(id, vol, vol, 1, 0, pitch)
        }
    }

    fun release() {
        soundPool.release()
        // Clean up temp wav files
        for (i in 0 until 4) {
            java.io.File(context.cacheDir, "pop_$i.wav").delete()
        }
    }
}
