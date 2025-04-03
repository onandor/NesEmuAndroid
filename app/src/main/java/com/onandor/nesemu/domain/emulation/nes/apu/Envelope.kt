package com.onandor.nesemu.domain.emulation.nes.apu

import com.onandor.nesemu.domain.emulation.savestate.EnvelopeState
import com.onandor.nesemu.domain.emulation.savestate.Savable

class Envelope : Clockable, Savable<EnvelopeState> {

    var isStarted: Boolean = false
    var isLooping: Boolean = false
    var isConstant: Boolean = false
    var volume: Int = 0

    val divider = Divider {
        if (volume > 0) {
            volume -= 1
        } else if (isLooping) {
            volume = 15
        }
    }

    fun getOutput(): Int = if (isConstant) divider.period else volume

    override fun clock() {
        if (!isStarted) {
            divider.clock()
        } else {
            isStarted = false
            volume = 15
            divider.reload()
        }
    }

    override fun reset() {
        isStarted = false
        isLooping = false
        isConstant = false
        volume = 0

        divider.reset()
    }

    override fun createSaveState(): EnvelopeState {
        return EnvelopeState(
            isStarted = isStarted,
            isLooping = isLooping,
            isConstant = isConstant,
            volume = volume,
            divider = divider.createSaveState()
        )
    }

    override fun loadState(state: EnvelopeState) {
        isStarted = state.isStarted
        isLooping = state.isLooping
        isConstant = state.isConstant
        volume = state.volume
        divider.loadState(state.divider)
    }
}