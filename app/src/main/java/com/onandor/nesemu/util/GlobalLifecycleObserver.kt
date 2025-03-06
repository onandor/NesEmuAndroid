package com.onandor.nesemu.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.onandor.nesemu.emulation.Emulator
import com.onandor.nesemu.input.NesInputManager
import javax.inject.Inject

class GlobalLifecycleObserver @Inject constructor(
    private val emulator: Emulator,
    private val inputManager: NesInputManager
) : DefaultLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        emulator.initAudioPlayer()
        inputManager.registerListener()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        emulator.destroyAudioPlayer()
        inputManager.unregisterListener()
    }
}