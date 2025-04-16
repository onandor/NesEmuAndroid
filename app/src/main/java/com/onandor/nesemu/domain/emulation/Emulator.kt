package com.onandor.nesemu.domain.emulation

import android.util.Log
import com.onandor.nesemu.domain.audio.AudioPlayer
import com.onandor.nesemu.di.DefaultDispatcher
import com.onandor.nesemu.domain.emulation.nes.Cartridge
import com.onandor.nesemu.domain.emulation.nes.Nes
import com.onandor.nesemu.domain.emulation.savestate.NesState
import com.onandor.nesemu.domain.service.InputService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Emulator @Inject constructor(
    @DefaultDispatcher private val coroutineScope: CoroutineScope,
    private val inputService: InputService
) {

    private companion object {
        const val TAG = "Emulator"
    }

    val nes = Nes(
        onFrameReady = ::notifyListenersFrameReady,
        onPollController1 = { inputService.getButtonStates(InputService.PLAYER_1) },
        onPollController2 = { inputService.getButtonStates(InputService.PLAYER_2) }
    )
    private lateinit var cartridge: Cartridge

    private val audioPlayer = AudioPlayer(::setAudioSampleRate, ::provideAudioSamples)
    private var nesRunnerJob: Job? = null
    private val listeners = mutableListOf<EmulationListener>()

    fun loadRom(rom: ByteArray) {
        cartridge = Cartridge()
        cartridge.parseRom(rom)
        nes.insertCartridge(cartridge)
    }

    private fun setAudioSampleRate(sampleRate: Int) {
        nes.apu.setSampleRate(sampleRate)
    }

    private fun provideAudioSamples(numSamples: Int): ShortArray {
        return nes.drainAudioBuffer(numSamples)
    }

    private fun notifyListenersFrameReady(
        frame: IntArray,
        patternTable: IntArray,
        nametable: IntArray,
        colorPalettes: Array<IntArray>
    ) {
        listeners.forEach { it.onFrameReady(frame, patternTable, nametable, colorPalettes) }
    }

    fun start() {
        if (!nes.running) {
            nesRunnerJob = coroutineScope.launch {
                try {
                    nes.run()
                } catch (e: Exception) {
                    Log.e(TAG, e.localizedMessage, e)
                    stop()
                }
            }
            audioPlayer.startStream()
        }
    }

    fun stop() {
        if (nes.running) {
            audioPlayer.pauseStream()
            nes.stop()
            nesRunnerJob?.cancel()
            runBlocking { nesRunnerJob?.join() }
        }
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

    fun initAudioPlayer() {
        audioPlayer.init()
    }

    fun destroyAudioPlayer() {
        audioPlayer.destroy()
    }

    fun createSaveState(): NesState = nes.createSaveState()

    fun loadSaveState(nesState: NesState) {
        if (nes.running) {
            stop()
        }
        nes.reset()
        nes.loadState(nesState)
    }
}