package com.onandor.nesemu.emulation.nes.apu

import com.onandor.nesemu.emulation.savestate.PulseChannelState
import com.onandor.nesemu.emulation.savestate.Savable

class PulseChannel(channel: Int) : Clockable, Savable<PulseChannelState> {
    var length: Int = 0
    var lengthFrozen: Boolean = false
    var dutyCycle: Int = DUTY_CYCLE_LOOKUP[0]
    var phase: Int = 0

    val divider = Divider { phase = (phase + 1) % 8 }
    val envelope = Envelope()
    val sweep = Sweep(this, channel)

    override fun reset() {
        length = 0
        lengthFrozen = false
        dutyCycle = DUTY_CYCLE_LOOKUP[0]
        phase = 0

        divider.reset()
        envelope.reset()
        sweep.reset()
    }

    override fun clock() {
        if (!lengthFrozen && length > 0) {
            length -= 1
        }
    }

    fun setControl(value: Int) {
        dutyCycle = DUTY_CYCLE_LOOKUP[(value and 0xC0) ushr 6]
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

    fun setSweep(value: Int) {
        sweep.isEnabled = value and 0x80 > 0
        sweep.divider.period = (value and 0x70) ushr 4
        sweep.isNegated = value and 0x08 > 0
        sweep.shiftCount = value and 0x07
    }

    fun setDividerLow(value: Int) {
        divider.period = (divider.period and 0x700) or (value and 0xFF)
    }

    fun setDividerHigh(value: Int) {
        divider.period = ((value and 0b111) shl 8) or (divider.period and 0xFF)
        divider.counter = divider.period
        phase = 0
        length = Apu.LENGTH_COUNTER_LOOKUP[(value and 0xF8) ushr 3]
        envelope.isStarted = true
    }

    fun getOutput(): Int {
        return if (sweep.muting() ||
            length == 0 ||
            (dutyCycle shl phase) and 0x80 == 0) {
            0
        } else {
            envelope.getOutput()
        }
    }

    override fun saveState(): PulseChannelState {
        return PulseChannelState(
            length = length,
            lengthFrozen = lengthFrozen,
            dutyCycle = dutyCycle,
            phase = phase,
            divider = divider.saveState(),
            envelope = envelope.saveState(),
            sweep = sweep.saveState()
        )
    }

    override fun loadState(state: PulseChannelState) {
        length = state.length
        lengthFrozen = state.lengthFrozen
        dutyCycle = state.dutyCycle
        phase = state.phase
        divider.loadState(state.divider)
        envelope.loadState(state.envelope)
        sweep.loadState(state.sweep)
    }

    companion object {
        private val DUTY_CYCLE_LOOKUP: Array<Int> = arrayOf(
            //  12.5%       25%         50%         75% (25% negated)
            0b00000001, 0b00000011, 0b00001111, 0b00111111
        )
    }
}