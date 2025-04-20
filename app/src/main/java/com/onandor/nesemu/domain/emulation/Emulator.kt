package com.onandor.nesemu.domain.emulation

import android.media.AudioManager
import android.util.Log
import com.onandor.nesemu.domain.audio.AudioPlayer
import com.onandor.nesemu.domain.emulation.nes.Cartridge
import com.onandor.nesemu.domain.emulation.nes.DebugFeature
import com.onandor.nesemu.domain.emulation.nes.Nes
import com.onandor.nesemu.domain.emulation.savestate.NesState
import java.util.concurrent.CountDownLatch
import kotlin.time.TimeSource

class Emulator (
    audioManager: AudioManager,
    private val onFrameReady: (Nes.Frame) -> Unit,
    private val onPollController1: () -> Int,
    private val onPollController2: () -> Int
) {

    private companion object {
        const val TAG = "Emulator"
    }

    private val nes = Nes(
        onPollController1 = onPollController1,
        onPollController2 = onPollController2
    )
    private lateinit var cartridge: Cartridge

    private val audioPlayer = AudioPlayer(audioManager)

    private var running: Boolean = false
    private var stopLatch = CountDownLatch(1)

    init {
        nes.apu.setSampleRate(audioPlayer.sampleRate)
    }

    fun loadRom(rom: ByteArray) {
        cartridge = Cartridge()
        cartridge.parseRom(rom)
        nes.insertCartridge(cartridge)
    }

    fun run() {
        audioPlayer.start()
        val timeSource = TimeSource.Monotonic
        var fpsMeasureStart = timeSource.markNow()
        var numFrames = 0
        running = true

        while (running) {
            val frameStart = timeSource.markNow()

            val frame = nes.generateFrame()
            val audioSamples = nes.drainAudioBuffer()

            onFrameReady(frame)
            audioPlayer.queueSamples(audioSamples)

            val now = timeSource.markNow()

            val sleepMicros = 1_000_000 / 60 - (now - frameStart).inWholeMicroseconds
            if (sleepMicros > 0) {
                val millis = sleepMicros / 1000
                val nanos = ((sleepMicros % 1_000) * 1_000).toInt()
                Thread.sleep(millis, nanos)
            }

            numFrames += 1

            if ((now - fpsMeasureStart).inWholeMilliseconds >= 3000) {
                val fps = numFrames / 3f
                numFrames = 0
                fpsMeasureStart = timeSource.markNow()
                Log.i(TAG, "FPS: $fps")
            }
        }

        stopLatch.countDown()
    }

    fun stop(blocking: Boolean) {
        if (!running) {
            return
        }
        running = false
        audioPlayer.stop()
        if (blocking) {
            stopLatch.await()
        }
        stopLatch = CountDownLatch(1)
    }

    fun reset() {
        nes.reset()
    }

    fun setDebugFeatureBool(feature: DebugFeature, value: Boolean) {
        nes.setDebugFeatureBool(feature, value)
    }

    fun setDebugFeatureInt(feature: DebugFeature, value: Int) {
        nes.setDebugFeatureInt(feature, value)
    }

    fun createSaveState(): NesState = nes.captureState()

    fun loadSaveState(nesState: NesState) {
        stop(true)
        nes.reset()
        nes.loadState(nesState)
    }
}