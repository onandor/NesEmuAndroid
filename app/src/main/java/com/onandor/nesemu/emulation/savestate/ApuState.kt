package com.onandor.nesemu.emulation.savestate

data class ApuState(
    val cpuCyclesSinceSample: Int,
    val cycles: Int,
    val cpuCycles: Int,
    val sequenceCycles: Int,
    val pulse1: PulseChannelState,
    val pulse2: PulseChannelState,
    val triangle: TriangleChannelState,
    val noise: NoiseChannelState,
    val dmc: DMCState
) : SaveState()

data class PulseChannelState(
    val length: Int,
    val lengthFrozen: Boolean,
    val dutyCycle: Int,
    val phase: Int,
    val divider: DividerState,
    val envelope: EnvelopeState,
    val sweep: SweepState
) : SaveState()

data class TriangleChannelState(
    val length: Int,
    val control: Boolean,
    val counter: Int,
    val reloadValue: Int,
    val reloadCounter: Boolean,
    val phase: Int,
    val divider: DividerState
) : SaveState()

data class NoiseChannelState(
    val length: Int,
    val lengthFrozen : Boolean,
    val mode: Boolean,
    val shifter: Int,
    val divider: DividerState,
    val envelope: EnvelopeState
) : SaveState()

data class DMCState(
    val interruptEnable: Boolean,
    val isLooping: Boolean,
    val isEnabled: Boolean,
    val sample : DMCSampleState,
    val output: DMCOutputState,
    val divider: DividerState
) : SaveState()

data class DMCSampleState(
    val address: Int,
    val startingAddress: Int,
    val length: Int,
    val bytesRemaining: Int,
    val buffer: Int,
    val isEmpty: Boolean
) : SaveState()

data class DMCOutputState(
    val bitsRemaining: Int,
    val level: Int,
    val isSilenced: Boolean,
    val shifter: Int
) : SaveState()

data class DividerState(
    val counter: Int,
    val period: Int
) : SaveState()

data class EnvelopeState(
    val isStarted: Boolean,
    val isLooping: Boolean,
    val isConstant: Boolean,
    val volume: Int,
    val divider: DividerState
) : SaveState()

data class SweepState(
    val isEnabled: Boolean,
    val isNegated: Boolean,
    val reload: Boolean,
    val shiftCount: Int,
    val targetPeriod: Int,
    val divider: DividerState
) : SaveState()