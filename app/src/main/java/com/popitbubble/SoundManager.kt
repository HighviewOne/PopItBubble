package com.popitbubble

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*
import kotlin.random.Random

class SoundManager(private val context: Context) {

    private val soundPool: SoundPool
    private val popSoundIds = mutableListOf<Int>()
    private val loadedSounds = mutableSetOf<Int>()
    private val currentIndex = AtomicInteger(0)
    private val scope = CoroutineScope(Dispatchers.IO)

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

        // Generate or load 4 pop sound variations asynchronously
        scope.launch {
            for (i in 0 until 4) {
                loadOrGenerateSound(i)
            }
        }
    }

    private fun loadOrGenerateSound(variation: Int) {
        val file = File(context.cacheDir, "pop_$variation.wav")
        
        if (!file.exists()) {
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
            file.writeBytes(wavBytes)
        }

        val id = soundPool.load(file.absolutePath, 1)
        if (id > 0) {
            synchronized(popSoundIds) {
                popSoundIds.add(id)
            }
        }
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
        val ids = synchronized(popSoundIds) { popSoundIds.toList() }
        if (ids.isEmpty()) return
        
        val index = currentIndex.getAndIncrement()
        val id = ids[index % ids.size]
        if (id in loadedSounds) {
            val pitch = 0.85f + Random.nextFloat() * 0.3f
            val vol = 0.8f + Random.nextFloat() * 0.2f
            soundPool.play(id, vol, vol, 1, 0, pitch)
        }
    }

    fun release() {
        soundPool.release()
    }
}
