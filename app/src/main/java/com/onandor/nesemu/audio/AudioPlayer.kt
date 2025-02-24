package com.onandor.nesemu.audio

import android.util.Log
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import java.nio.ByteBuffer
import kotlin.random.Random

@OptIn(InternalCoroutinesApi::class)
class AudioPlayer(
    private val onSampleRateAcquired: (Int) -> Unit,
    private val onSamplesRequested: (Int) -> FloatArray
) {

    private companion object {
        const val TAG = "AudioPlayer"
        init {
            System.loadLibrary("audio-player")
        }
    }

    private var nativeInstanceHandle: Long = -1L
    private val mutex: Object = Object()
    private var isStreamRunning: Boolean = false

    private external fun create(): Long
    private external fun delete(handle: Long)
    private external fun startStream(handle: Long)
    private external fun pauseStream(handle: Long)
    private external fun getSampleRate(handle: Long): Int

    fun onSamplesRequested(buffer: ByteBuffer): Int {
        val floatBuffer = buffer.asFloatBuffer()
        var samples: FloatArray = onSamplesRequested(floatBuffer.capacity())
        floatBuffer.put(samples)
        return samples.size
    }

    fun startStream() {
        synchronized(mutex) {
            startStream(nativeInstanceHandle)
            isStreamRunning = true
        }
    }

    fun pauseStream() {
        synchronized(mutex) {
            pauseStream(nativeInstanceHandle)
            isStreamRunning = false
        }
    }

    fun init() {
        synchronized(mutex) {
            val firstInit = if (nativeInstanceHandle > 0L) return else nativeInstanceHandle == -1L
            nativeInstanceHandle = create()
            Log.d(TAG, "Native AudioPlayer created")

            if (firstInit) {
                val sampleRate = getSampleRate(nativeInstanceHandle)
                onSampleRateAcquired(sampleRate)
                Log.d(TAG, "Sample rate set as $sampleRate")
            }
            if (isStreamRunning) {
                startStream(nativeInstanceHandle)
            }
        }
    }

    fun destroy() {
        synchronized(mutex) {
            if (nativeInstanceHandle <= 0L) {
                return
            }

            delete(nativeInstanceHandle)
            nativeInstanceHandle = 0L
            Log.d(TAG, "Native AudioPlayer destroyed")
        }
    }
}