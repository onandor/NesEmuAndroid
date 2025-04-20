package com.onandor.nesemu.domain.emulation.savestate

import kotlinx.serialization.Serializable

@Serializable
data class ApuState(
    val sequenceCycles: Int,
    val interrupt: Boolean,
    val interruptEnable: Boolean,
    val cpuCycles: Int,
    val cycles: Int,
    val pulse1: PulseChannelState,
    val pulse2: PulseChannelState,
    val triangle: TriangleChannelState,
    val noise: NoiseChannelState,
    val dmc: DmcState
) : SaveState()

@Serializable
data class PulseChannelState(
    val enabled: Boolean,
    val sequencer: Int,
    val sequencePhase: Int,
    val timer: Int,
    val timerPeriod: Int,
    val envelope: EnvelopeState,
    val lengthCounter: LengthCounterState,
    val sweep: SweepState
) : SaveState()

@Serializable
data class TriangleChannelState(
    val enabled: Boolean,
    val sequencer: Int,
    val timer: Int,
    val timerPeriod: Int,
    val linearCounter: Int,
    val linearCounterPeriod: Int,
    val reloadLinearCounter: Boolean,
    val controlFlag: Boolean,
    val lengthCounter: LengthCounterState
) : SaveState()

@Serializable
data class NoiseChannelState(
    val enabled: Boolean,
    val timer: Int,
    val timerPeriod: Int,
    val shifter: Int,
    val mode: Boolean,
    val envelope: EnvelopeState,
    val lengthCounter: LengthCounterState
) : SaveState()

@Serializable
data class DmcState(
    val enabled: Boolean,
    val timer: Int,
    val timerPeriod: Int,
    val interruptEnable: Boolean,
    val interrupt: Boolean,
    val loop: Boolean,
    val reader : DmcReaderState,
    val output: DmcOutputState
) : SaveState()

@Serializable
data class DmcReaderState(
    val address: Int,
    val startingAddress: Int,
    val length: Int,
    val bytesRemaining: Int,
    val buffer: Int,
    val bufferEmpty: Boolean
) : SaveState()

@Serializable
data class DmcOutputState(
    val bitsRemaining: Int,
    val level: Int,
    val silenced: Boolean,
    val shifter: Int
) : SaveState()

@Serializable
data class EnvelopeState(
    val start: Boolean,
    val divider: Int,
    val volume: Int,
    val decay: Int,
    val constant: Boolean,
    val loop: Boolean
) : SaveState()

@Serializable
data class SweepState(
    val divider: Int,
    val dividerPeriod: Int,
    val targetPulsePeriod: Int,
    val shiftCount: Int,
    val reload: Boolean,
    val negate: Boolean,
    val enabled: Boolean
) : SaveState()

@Serializable
data class LengthCounterState(
    val length: Int,
    val halt: Boolean
) : SaveState()