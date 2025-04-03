package com.onandor.nesemu.domain.emulation.nes.apu

import com.onandor.nesemu.domain.emulation.savestate.NoiseChannelState
import com.onandor.nesemu.domain.emulation.savestate.Savable

class NoiseChannel : Clockable, Savable<NoiseChannelState> {

    var length: Int = 0
    var lengthFrozen: Boolean = false
    var mode: Boolean = false
    var shifter: Int = 0b000_0000_0000_0001

    var divider = Divider {
        val shift = if (mode) 6 else 1
        val feedback = (shifter and 0x01) xor ((shifter shr shift) and 0x01)
        shifter = (shifter ushr 1) or (feedback shl 14)
    }
    var envelope = Envelope()

    override fun clock() {
        if (!lengthFrozen && length > 0) {
            length -= 1
        }
    }

    override fun reset() {
        length = 0
        lengthFrozen = false
        mode = false
        shifter = 0b000_0000_0000_0001

        divider.reset()
        envelope.reset()
    }

    fun setControl(value: Int) {
        lengthFrozen = value and 0x20 > 0
        envelope.isLooping = value and 0x20 > 0
        envelope.isConstant = value and 0x10 > 0
        if (envelope.isConstant) {
            envelope.divider.period = value and 0x0F
        } else {
            envelope.volume = value and 0x0F
        }
        envelope.isStarted = true
    }

    fun setDivider(value: Int) {
        mode = value and 0x80 > 0
        divider.period = DIVIDER_PERIOD_LOOKUP[value and 0x0F]
    }

    fun setLengthAndEnvelope(value: Int) {
        length = Apu.LENGTH_COUNTER_LOOKUP[(value and 0xF8) ushr 3]
        envelope.isStarted = true
    }

    fun getOutput(): Int {
        return if (shifter and 0x01 > 0 || length == 0) {
            0
        } else {
            envelope.getOutput()
        }
    }

    override fun createSaveState(): NoiseChannelState {
        return NoiseChannelState(
            length = length,
            lengthFrozen = lengthFrozen,
            mode = mode,
            shifter = shifter,
            divider = divider.createSaveState(),
            envelope = envelope.createSaveState()
        )
    }

    override fun loadState(state: NoiseChannelState) {
        length = state.length
        lengthFrozen = state.lengthFrozen
        mode = state.mode
        shifter = state.shifter
        divider.loadState(state.divider)
        envelope.loadState(state.envelope)
    }

    companion object {
        private val DIVIDER_PERIOD_LOOKUP: IntArray = intArrayOf(
            4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068
        )
    }
}