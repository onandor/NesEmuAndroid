package com.onandor.nesemu.domain.emulation

import android.media.AudioManager
import android.util.Log
import com.onandor.nesemu.domain.audio.AudioPlayer
import com.onandor.nesemu.domain.emulation.nes.Cartridge
import com.onandor.nesemu.domain.emulation.nes.Nes
import com.onandor.nesemu.domain.emulation.savestate.NesState
import com.onandor.nesemu.domain.service.InputService
import kotlinx.coroutines.delay
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.TimeSource

@Singleton
class Emulator @Inject constructor(
    audioManager: AudioManager,
    private val inputService: InputService
) {

    private companion object {
        const val TAG = "Emulator"
    }

    val nes = Nes(
        onPollController1 = { inputService.getButtonStates(InputService.PLAYER_1) },
        onPollController2 = { inputService.getButtonStates(InputService.PLAYER_2) }
    )
    private lateinit var cartridge: Cartridge

    private val audioPlayer = AudioPlayer(audioManager)
    private val listeners = mutableListOf<EmulationListener>()

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

    private fun setAudioSampleRate(sampleRate: Int) {
        nes.apu.setSampleRate(sampleRate)
    }

    suspend fun run() {
        audioPlayer.start()
        val timeSource = TimeSource.Monotonic
        running = true

        while (running) {
            val frameStart = timeSource.markNow()

            val frame = nes.generateFrame()
            val audioSamples = nes.drainAudioBuffer()

            listeners.forEach { it.onFrameReady(frame) }
            audioPlayer.queueSamples(audioSamples)

            val now = timeSource.markNow()
            delay(1000 / 60 - (now - frameStart).inWholeMilliseconds)
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

    fun registerListener(listener: EmulationListener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: EmulationListener) {
        listeners.remove(listener)
    }

    fun destroyAudioPlayer() {
        audioPlayer.destroy()
    }

    fun createSaveState(): NesState = nes.createSaveState()

    fun loadSaveState(nesState: NesState) {
        stop(true)
        nes.reset()
        nes.loadState(nesState)
    }
}