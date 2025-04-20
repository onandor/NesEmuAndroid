package com.onandor.nesemu.domain.emulation.nes.apu

import com.onandor.nesemu.domain.emulation.savestate.LengthCounterState
import com.onandor.nesemu.domain.emulation.savestate.Savable

// https://www.nesdev.org/wiki/APU_Length_Counter
// The length counter controls the duration of the waveform channels. It is clocked on every half frame.
// When clocked, it decreases the length value until it reaches zero. If zero, the output of the corresponding channel
// becomes muted until the length counter is optionally loaded with a new value from the lookup table and restarted.

class LengthCounter : Savable<LengthCounterState> {

    var length: Int = 0
    var halt: Boolean = false

    fun clock() {
        if (length > 0 && !halt) {
            length -= 1
        }
    }

    fun load(index: Int) {
        length = LENGTH_VALUE_LOOKUP[index]
    }

    fun reset() {
        length = 0
        halt = false
    }

    override fun captureState(): LengthCounterState {
        return LengthCounterState(
            length = length,
            halt = halt
        )
    }

    override fun loadState(state: LengthCounterState) {
        length = state.length
        halt = state.halt
    }

    companion object {
        private val LENGTH_VALUE_LOOKUP = listOf(
            10,254, 20,  2, 40,  4, 80,  6, 160,  8, 60, 10, 14, 12, 26, 14,
            12, 16, 24, 18, 48, 20, 96, 22, 192, 24, 72, 26, 16, 28, 32, 30
        )
    }
}