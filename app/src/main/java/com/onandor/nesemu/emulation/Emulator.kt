package com.onandor.nesemu.emulation

import com.onandor.nesemu.audio.AudioPlayer
import com.onandor.nesemu.emulation.nes.Cartridge
import com.onandor.nesemu.emulation.nes.Nes
import com.onandor.nesemu.input.NesInputManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class Emulator @Inject constructor(private val inputManager: NesInputManager) {

    private companion object {
        const val TAG = "Emulator"
    }

    val nes = Nes(
        onFrameReady = ::notifyListenersFrameReady,
        onPollController1 = { inputManager.getButtonStates(NesInputManager.CONTROLLER_1) },
        onPollController2 = { inputManager.getButtonStates(NesInputManager.CONTROLLER_2) }
    )
    private lateinit var cartridge: Cartridge
    private val audioPlayer = AudioPlayer(::setAudioSampleRate, ::provideAudioSamples)

    private var nesRunnerJob: Job? = null

    private val listeners = mutableListOf<EmulationListener>()

    fun parseAndInsertRom(rom: ByteArray) {
        cartridge = Cartridge()
        cartridge.parseRom(rom)
        nes.insertCartridge(cartridge)
    }

    private fun setAudioSampleRate(sampleRate: Int) {
        nes.apu.setSampleRate(sampleRate)
    }

    private fun provideAudioSamples(numSamples: Int): FloatArray {
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
        nesRunnerJob = CoroutineScope(Dispatchers.Default).launch {
            nes.run()
        }
    }

    fun reset() {
        nes.reset()
        start()
    }

    fun stop() {
        nes.stop()
        nesRunnerJob?.cancel()
        runBlocking {
            nesRunnerJob?.join()
        }
    }

    fun registerListener(listener: EmulationListener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: EmulationListener) {
        listeners.remove(listener)
    }

    fun startAudioStream() {
        audioPlayer.startStream()
    }

    fun pauseAudioStream() {
        audioPlayer.pauseStream()
    }

    fun initAudioPlayer() {
        audioPlayer.init()
    }

    fun destroyAudioPlayer() {
        audioPlayer.destroy()
    }
}