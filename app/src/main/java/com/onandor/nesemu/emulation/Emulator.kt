package com.onandor.nesemu.emulation

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.onandor.nesemu.audio.AudioPlayer
import com.onandor.nesemu.emulation.nes.Cartridge
import com.onandor.nesemu.emulation.nes.Nes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class Emulator : DefaultLifecycleObserver {

    private companion object {
        const val TAG = "Emulator"
    }

    val nes = Nes()
    val audioPlayer = AudioPlayer()
    lateinit var cartridge: Cartridge

    private var nesRunnerJob: Job? = null

    fun parseAndInsertRom(rom: ByteArray) {
        cartridge = Cartridge()
        cartridge.parseRom(rom)
        nes.insertCartridge(cartridge)
    }

    fun reset() {
        nesRunnerJob = CoroutineScope(Dispatchers.Default).launch {
            nes.reset()
        }
    }

    fun stop() {
        nes.running = false
        nesRunnerJob?.cancel()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        audioPlayer.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        audioPlayer.onPause()
    }
}