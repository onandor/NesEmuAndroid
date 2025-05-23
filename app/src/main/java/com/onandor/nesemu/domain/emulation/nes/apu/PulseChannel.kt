package com.onandor.nesemu.domain.emulation.nes.apu

import com.onandor.nesemu.domain.emulation.savestate.PulseChannelState
import com.onandor.nesemu.domain.emulation.savestate.Savable

// https://www.nesdev.org/wiki/APU_Pulse
// The pulse channel outputs a square waveform with variable duty.

class PulseChannel(val channelNumber: Int) : Savable<PulseChannelState> {

    private var enabled: Boolean = false

    // The sequencer holds an 8 bit value representing the duty cycle of the generated waveform.
    private var sequencer: Int = 0
    private var sequencePhase: Int = 0

    // The timer has an 11 bit long period, which is decreased every APU cycle
    // When it reaches zero, it clocks the sequencer, and is automatically reloaded with the value
    // stored in timerPeriod
    private var timer: Int = 0
    var timerPeriod: Int = 0

    private val envelope = Envelope()
    val lengthCounter = LengthCounter()
    private val sweep = Sweep(this)

    fun clockTimer() {
        timer -= 1
        if (timer == -1) {
            timer = timerPeriod
            sequencePhase = (sequencePhase + 1) % 8
        }
    }

    fun clockEnvelope() {
        envelope.clock()
    }

    fun clockLengthCounter() {
        lengthCounter.clock()
    }

    fun clockSweep() {
        sweep.clock()
    }

    fun reset() {
        enabled = false
        timer = 0
        timerPeriod = 0
        sequencer = 0
        sequencePhase = 0
        envelope.reset()
        lengthCounter.reset()
        sweep.reset()
    }

    // Registers
    // https://www.nesdev.org/wiki/APU#Specification
    // https://www.nesdev.org/wiki/APU_registers

    // 0x4000 / 0x4004
    fun writeControl(value: Int) {
        sequencer = SEQUENCE_LOOKUP[(value ushr 6)]
        lengthCounter.halt = (value and 0x20) != 0
        envelope.loop = (value and 0x20) != 0
        envelope.constant = (value and 0x10) != 0
        envelope.volume = value and 0x0F
    }

    // 0x4001 / 0x4005
    fun writeSweep(value: Int) {
        sweep.load(value)
    }

    // 0x4002 / 0x4006
    fun writeTimer(value: Int) {
        timerPeriod = (timerPeriod and 0x700) or value
        sweep.updateTargetPeriod()
    }

    // 0x4003 / 0x4007
    fun writeLengthCounter(value: Int) {
        timerPeriod = ((value and 0x07) shl 8) or (timerPeriod and 0x00FF)
        sweep.updateTargetPeriod()
        lengthCounter.load(value ushr 3)
        envelope.start = true
        sequencePhase = 0
    }

    // 0x4015
    fun writeEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) {
            lengthCounter.length = 0
        }
    }

    // https://www.nesdev.org/wiki/APU_Pulse#Pulse_channel_output_to_mixer
    fun getOutput(): Int {
        val silenced = (sequencer shl sequencePhase) and 0x80 == 0 ||
                lengthCounter.length == 0 || sweep.isMuting()|| !enabled
        return if (silenced) 0 else envelope.getOutput()
    }

    override fun captureState(): PulseChannelState {
        return PulseChannelState(
            enabled = enabled,
            sequencer = sequencer,
            sequencePhase = sequencePhase,
            timer = timer,
            timerPeriod = timerPeriod,
            envelope = envelope.captureState(),
            lengthCounter = lengthCounter.captureState(),
            sweep = sweep.captureState()
        )
    }

    override fun loadState(state: PulseChannelState) {
        enabled = state.enabled
        sequencer = state.sequencer
        sequencePhase = state.sequencePhase
        timer = state.timer
        timerPeriod = state.timerPeriod
        envelope.loadState(state.envelope)
        lengthCounter.loadState(state.lengthCounter)
        sweep.loadState(state.sweep)
    }

    companion object {
        const val CHANNEL_1 = 1
        const val CHANNEL_2 = 2

        private val SEQUENCE_LOOKUP: Array<Int> = arrayOf(
            //  12.5%        25%          50%          75%
            0b0000_0001, 0b0000_0011, 0b0000_1111, 0b1111_1100
        )
    }
}