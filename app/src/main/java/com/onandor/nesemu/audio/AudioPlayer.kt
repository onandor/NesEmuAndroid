package com.onandor.nesemu.audio

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.withContext

@OptIn(InternalCoroutinesApi::class)
class AudioPlayer {

    private var nativeInstanceHandle: Long = 0
    private val mutex: Object = Object()

    companion object {
        const val TAG = "AudioPlayer"
        init {
            System.loadLibrary("audio-player")
        }
    }

    private external fun create(): Long
    private external fun delete(handle: Long)
    private external fun playSound(handle: Long)

    suspend fun playSound() = withContext(Dispatchers.Default) {
        synchronized(mutex) {
            //createNativeInstance()
            playSound(nativeInstanceHandle)
        }
    }

    private fun createNativeInstance(): Boolean {
        if (nativeInstanceHandle != 0L) {
            return false
        }
        nativeInstanceHandle = create()
        return true
    }

    fun onResume() {
        synchronized(mutex) {
            if (createNativeInstance()) {
                Log.d(TAG, "Created")
            }
        }
    }

    fun onPause() {
        synchronized(mutex) {
            if (nativeInstanceHandle == 0L) {
                return
            }

            delete(nativeInstanceHandle)
            nativeInstanceHandle = 0L
            Log.d(TAG, "Destroyed")
        }
    }
}